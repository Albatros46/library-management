package com.library.catalog;

import com.library.catalog.BookDtos.BookRequest;
import com.library.catalog.BookDtos.BookView;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookRepository books;
    private final AuthorRepository authors;
    private final PublisherRepository publishers;
    private final MongoTemplate mongo;

    public BookController(BookRepository books, AuthorRepository authors,
                          PublisherRepository publishers, MongoTemplate mongo) {
        this.books = books;
        this.authors = authors;
        this.publishers = publishers;
        this.mongo = mongo;
    }

    @GetMapping
    public List<BookView> list(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String authorId,
                               @RequestParam(required = false) String publisherId,
                               @RequestParam(defaultValue = "false") boolean availableOnly) {
        Query query = new Query().with(Sort.by("title"));
        if (StringUtils.hasText(q)) {
            String rx = Pattern.quote(q.trim());
            List<String> matchingAuthors = authors.findByNameContainingIgnoreCaseOrderByName(q.trim())
                    .stream().map(Author::getId).toList();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(rx, "i"),
                    Criteria.where("isbn").regex(rx, "i"),
                    Criteria.where("authorId").in(matchingAuthors)));
        }
        if (StringUtils.hasText(category)) query.addCriteria(Criteria.where("category").is(category));
        if (StringUtils.hasText(authorId)) query.addCriteria(Criteria.where("authorId").is(authorId));
        if (StringUtils.hasText(publisherId)) query.addCriteria(Criteria.where("publisherId").is(publisherId));
        if (availableOnly) query.addCriteria(Criteria.where("availableCopies").gt(0));
        return toViews(mongo.find(query, Book.class));
    }

    @GetMapping("/{id}")
    public BookView get(@PathVariable String id) {
        return toViews(List.of(find(id))).getFirst();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return mongo.findDistinct(new Query(), "category", Book.class, String.class)
                .stream().sorted().toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookView create(@Valid @RequestBody BookRequest req) {
        Book b = new Book();
        apply(b, req);
        b.setAvailableCopies(req.totalCopies());
        return get(books.save(b).getId());
    }

    @PutMapping("/{id}")
    public BookView update(@PathVariable String id, @Valid @RequestBody BookRequest req) {
        Book b = find(id);
        int onLoan = b.getTotalCopies() - b.getAvailableCopies();
        if (req.totalCopies() < onLoan) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ödünçte " + onLoan + " kopya var; toplam kopya bundan az olamaz");
        }
        apply(b, req);
        b.setAvailableCopies(req.totalCopies() - onLoan);
        return get(books.save(b).getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        Book b = find(id);
        if (b.getAvailableCopies() < b.getTotalCopies()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ödünçte kopyası olan kitap silinemez");
        }
        books.delete(b);
    }

    /** Called by loan-service when a loan is approved: atomically takes one copy. */
    @PostMapping("/{id}/checkout")
    public BookView checkout(@PathVariable String id) {
        Book updated = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("availableCopies").gt(0)),
                new Update().inc("availableCopies", -1),
                FindAndModifyOptions.options().returnNew(true), Book.class);
        if (updated == null) {
            find(id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kitabın müsait kopyası yok");
        }
        return get(id);
    }

    /** Called by loan-service when a return is approved: puts one copy back. */
    @PostMapping("/{id}/return")
    public BookView giveBack(@PathVariable String id) {
        Book b = find(id);
        mongo.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("availableCopies").lt(b.getTotalCopies())),
                new Update().inc("availableCopies", 1), Book.class);
        return get(id);
    }

    private void apply(Book b, BookRequest req) {
        if (!authors.existsById(req.authorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yazar bulunamadı");
        }
        if (!publishers.existsById(req.publisherId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yayınevi bulunamadı");
        }
        b.setTitle(req.title().trim());
        b.setIsbn(req.isbn());
        b.setAuthorId(req.authorId());
        b.setPublisherId(req.publisherId());
        b.setPageCount(req.pageCount());
        b.setCategory(req.category().trim());
        b.setPublishYear(req.publishYear());
        b.setDescription(req.description());
        b.setTotalCopies(req.totalCopies());
    }

    private Book find(String id) {
        return books.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kitap bulunamadı"));
    }

    List<BookView> toViews(Collection<Book> list) {
        Map<String, Author> a = authors.findAllById(list.stream().map(Book::getAuthorId).distinct().toList())
                .stream().collect(Collectors.toMap(Author::getId, Function.identity()));
        Map<String, Publisher> p = publishers.findAllById(list.stream().map(Book::getPublisherId).distinct().toList())
                .stream().collect(Collectors.toMap(Publisher::getId, Function.identity()));
        return list.stream().map(b -> BookView.of(b, a.get(b.getAuthorId()), p.get(b.getPublisherId()))).toList();
    }
}
