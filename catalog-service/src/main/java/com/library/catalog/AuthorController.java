package com.library.catalog;

import com.library.catalog.BookDtos.AuthorDetail;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {
    private final AuthorRepository authors;
    private final BookRepository books;
    private final BookController bookController;

    public AuthorController(AuthorRepository authors, BookRepository books, BookController bookController) {
        this.authors = authors;
        this.books = books;
        this.bookController = bookController;
    }

    @GetMapping
    public List<Author> list(@RequestParam(required = false) String q) {
        return StringUtils.hasText(q)
                ? authors.findByNameContainingIgnoreCaseOrderByName(q.trim())
                : authors.findAll(Sort.by("name"));
    }

    /** Author info together with every book they wrote. */
    @GetMapping("/{id}")
    public AuthorDetail get(@PathVariable String id) {
        Author a = find(id);
        return new AuthorDetail(a, bookController.toViews(books.findByAuthorIdOrderByPublishYear(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Author create(@Valid @RequestBody Author author) {
        author.setId(null);
        return authors.save(author);
    }

    @PutMapping("/{id}")
    public Author update(@PathVariable String id, @Valid @RequestBody Author author) {
        find(id);
        author.setId(id);
        return authors.save(author);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (books.countByAuthorId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kitabı olan yazar silinemez");
        }
        authors.deleteById(id);
    }

    private Author find(String id) {
        return authors.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yazar bulunamadı"));
    }
}
