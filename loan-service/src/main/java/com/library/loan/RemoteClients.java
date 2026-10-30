package com.library.loan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

/** REST calls to member-service and catalog-service. */
@Component
public class RemoteClients {
    private final RestClient members;
    private final RestClient catalog;

    public RemoteClients(RestClient.Builder builder,
                         @Value("${library.services.member}") String memberUrl,
                         @Value("${library.services.catalog}") String catalogUrl) {
        this.members = builder.clone().baseUrl(memberUrl).build();
        this.catalog = builder.clone().baseUrl(catalogUrl).build();
    }

    public record MemberDto(String id, String firstName, String lastName, String email, String status) {
        String fullName() { return firstName + " " + lastName; }
    }

    public record Ref(String id, String name) {}

    public record BookDto(String id, String title, Ref author, String category, int pageCount, int availableCopies) {}

    public MemberDto member(String id) {
        return call("Üye", () -> members.get().uri("/api/members/{id}", id).retrieve().body(MemberDto.class));
    }

    public BookDto book(String id) {
        return call("Kitap", () -> catalog.get().uri("/api/books/{id}", id).retrieve().body(BookDto.class));
    }

    public BookDto checkout(String bookId) {
        return call("Kitap", () -> catalog.post().uri("/api/books/{id}/checkout", bookId).retrieve().body(BookDto.class));
    }

    public BookDto giveBack(String bookId) {
        return call("Kitap", () -> catalog.post().uri("/api/books/{id}/return", bookId).retrieve().body(BookDto.class));
    }

    /** Turns downstream errors into errors the caller of loan-service can understand. */
    private static <T> T call(String what, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, what + " bulunamadı");
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), what + " servisi isteği reddetti: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, what + " servisine ulaşılamıyor");
        }
    }
}
