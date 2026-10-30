package com.library.catalog;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/publishers")
public class PublisherController {
    private final PublisherRepository publishers;
    private final BookRepository books;

    public PublisherController(PublisherRepository publishers, BookRepository books) {
        this.publishers = publishers;
        this.books = books;
    }

    @GetMapping
    public List<Publisher> list() {
        return publishers.findAll(Sort.by("name"));
    }

    @GetMapping("/{id}")
    public Publisher get(@PathVariable String id) {
        return publishers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yayınevi bulunamadı"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Publisher create(@Valid @RequestBody Publisher publisher) {
        publisher.setId(null);
        return publishers.save(publisher);
    }

    @PutMapping("/{id}")
    public Publisher update(@PathVariable String id, @Valid @RequestBody Publisher publisher) {
        get(id);
        publisher.setId(id);
        return publishers.save(publisher);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (books.countByPublisherId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kitabı olan yayınevi silinemez");
        }
        publishers.deleteById(id);
    }
}
