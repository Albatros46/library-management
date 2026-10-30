package com.library.loan;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {
    List<Loan> findByMemberIdOrderByRequestedAtDesc(String memberId);
    List<Loan> findByMemberIdAndStatusInOrderByDueDate(String memberId, Collection<LoanStatus> statuses);
    List<Loan> findByStatusInOrderByRequestedAt(Collection<LoanStatus> statuses);
    List<Loan> findByStatusInOrderByDueDate(Collection<LoanStatus> statuses);
    List<Loan> findByStatusOrderByDueDate(LoanStatus status);
    List<Loan> findByBookIdAndStatusIn(String bookId, Collection<LoanStatus> statuses);
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);
    List<Loan> findByStatusAndDueDateBetweenOrderByDueDate(LoanStatus status, LocalDate from, LocalDate to);
    long countByMemberIdAndStatusIn(String memberId, Collection<LoanStatus> statuses);
    boolean existsByMemberIdAndBookIdAndStatusIn(String memberId, String bookId, Collection<LoanStatus> statuses);
}
