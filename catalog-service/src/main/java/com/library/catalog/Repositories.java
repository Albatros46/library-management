package com.library.catalog;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

interface AuthorRepository extends MongoRepository<Author, String> {
    List<Author> findByNameContainingIgnoreCaseOrderByName(String name);
}

interface PublisherRepository extends MongoRepository<Publisher, String> {
}

interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findByAuthorIdOrderByPublishYear(String authorId);
    long countByAuthorId(String authorId);
    long countByPublisherId(String publisherId);
}
