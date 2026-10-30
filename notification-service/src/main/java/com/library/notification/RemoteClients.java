package com.library.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

/** REST calls to loan-service and member-service. */
@Component
public class RemoteClients {
    private final RestClient loans;
    private final RestClient members;

    public RemoteClients(RestClient.Builder builder,
                         @Value("${library.services.loan}") String loanUrl,
                         @Value("${library.services.member}") String memberUrl) {
        this.loans = builder.clone().baseUrl(loanUrl).build();
        this.members = builder.clone().baseUrl(memberUrl).build();
    }

    public record LoanDto(String id, String memberId, String memberName, String memberEmail,
                          String bookId, String bookTitle, String authorName,
                          LocalDate loanDate, LocalDate dueDate, String status) {}

    public record MemberDto(String id, String firstName, String lastName, String email, boolean emailNotifications) {}

    private static final ParameterizedTypeReference<List<LoanDto>> LOAN_LIST = new ParameterizedTypeReference<>() {};

    public List<LoanDto> overdueLoans() {
        return loans.get().uri("/api/loans/overdue").retrieve().body(LOAN_LIST);
    }

    public List<LoanDto> dueSoon(int days) {
        return loans.get().uri("/api/loans/due-soon?days={d}", days).retrieve().body(LOAN_LIST);
    }

    public LoanDto loan(String id) {
        return loans.get().uri("/api/loans/{id}", id).retrieve().body(LoanDto.class);
    }

    public void markReminded(String loanId) {
        loans.post().uri("/api/loans/{id}/reminded", loanId).retrieve().toBodilessEntity();
    }

    public MemberDto member(String id) {
        return members.get().uri("/api/members/{id}", id).retrieve().body(MemberDto.class);
    }
}
