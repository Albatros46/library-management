package com.library.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class BookDtos {
    private BookDtos() {}

    public record BookRequest(@NotBlank String title,
                              String isbn,
                              @NotBlank String authorId,
                              @NotBlank String publisherId,
                              @Min(1) int pageCount,
                              @NotBlank String category,
                              Integer publishYear,
                              String description,
                              @Min(0) int totalCopies) {}

    public record AuthorRef(String id, String name) {}

    public record PublisherRef(String id, String name) {}

    /** Book with author and publisher names resolved, as the UI needs them. */
    public record BookView(String id, String title, String isbn, AuthorRef author, PublisherRef publisher,
                           int pageCount, String category, Integer publishYear, String description,
                           int totalCopies, int availableCopies) {
        static BookView of(Book b, Author a, Publisher p) {
            return new BookView(b.getId(), b.getTitle(), b.getIsbn(),
                    a == null ? new AuthorRef(b.getAuthorId(), "?") : new AuthorRef(a.getId(), a.getName()),
                    p == null ? new PublisherRef(b.getPublisherId(), "?") : new PublisherRef(p.getId(), p.getName()),
                    b.getPageCount(), b.getCategory(), b.getPublishYear(), b.getDescription(),
                    b.getTotalCopies(), b.getAvailableCopies());
        }
    }

    public record AuthorDetail(Author author, List<BookView> books) {}
}
