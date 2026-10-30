package com.library.loan;

import com.library.loan.Loan.StaffRef;
import com.library.loan.RemoteClients.BookDto;
import com.library.loan.RemoteClients.MemberDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class LoanService {
    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loans;
    private final RemoteClients remote;
    private final Clock clock;
    private final int defaultLoanDays;
    private final int maxActiveLoans;
    private final int maxExtensions;

    public LoanService(LoanRepository loans, RemoteClients remote, Clock clock,
                       @Value("${library.loan.default-days:14}") int defaultLoanDays,
                       @Value("${library.loan.max-active:5}") int maxActiveLoans,
                       @Value("${library.loan.max-extensions:1}") int maxExtensions) {
        this.loans = loans;
        this.remote = remote;
        this.clock = clock;
        this.defaultLoanDays = defaultLoanDays;
        this.maxActiveLoans = maxActiveLoans;
        this.maxExtensions = maxExtensions;
    }

    /** Staff opens a lending request; it waits in PENDING until a staff member approves it. */
    public Loan request(String memberId, String bookId, Integer days, String note, StaffRef staff) {
        MemberDto member = remote.member(memberId);
        if (!"ACTIVE".equals(member.status())) {
            throw conflict("Üye aktif değil, ödünç verilemez");
        }
        if (loans.countByMemberIdAndStatusIn(memberId, EnumSet.of(LoanStatus.OVERDUE)) > 0) {
            throw conflict("Üyenin iade süresi geçmiş kitabı var; önce iade alınmalı");
        }
        Set<LoanStatus> open = EnumSet.of(LoanStatus.PENDING, LoanStatus.ACTIVE, LoanStatus.OVERDUE, LoanStatus.RETURN_PENDING);
        if (loans.countByMemberIdAndStatusIn(memberId, open) >= maxActiveLoans) {
            throw conflict("Üye en fazla " + maxActiveLoans + " kitap ödünç alabilir");
        }
        if (loans.existsByMemberIdAndBookIdAndStatusIn(memberId, bookId, open)) {
            throw conflict("Bu kitap için üyenin açık bir ödünç kaydı zaten var");
        }
        BookDto book = remote.book(bookId);
        if (book.availableCopies() <= 0) {
            throw conflict("Kitabın müsait kopyası yok");
        }

        Loan loan = new Loan();
        loan.setMemberId(member.id());
        loan.setMemberName(member.fullName());
        loan.setMemberEmail(member.email());
        loan.setBookId(book.id());
        loan.setBookTitle(book.title());
        if (book.author() != null) {
            loan.setAuthorId(book.author().id());
            loan.setAuthorName(book.author().name());
        }
        loan.setCategory(book.category());
        loan.setPageCount(book.pageCount());
        loan.setStatus(LoanStatus.PENDING);
        loan.setRequestedBy(staff);
        loan.setRequestedAt(Instant.now(clock));
        loan.setDueDate(LocalDate.now(clock).plusDays(days == null ? defaultLoanDays : days));
        loan.setNote(note);
        return loans.save(loan);
    }

    /** Approves a lending: takes a copy from the catalog and starts the loan period from today. */
    public Loan approve(String id, StaffRef staff) {
        Loan loan = find(id);
        expect(loan, LoanStatus.PENDING);
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.ofInstant(loan.getRequestedAt(), clock.getZone()), loan.getDueDate());
        remote.checkout(loan.getBookId());
        LocalDate today = LocalDate.now(clock);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setApprovedBy(staff);
        loan.setApprovedAt(Instant.now(clock));
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(Math.max(days, 1)));
        return loans.save(loan);
    }

    public Loan reject(String id, String reason, StaffRef staff) {
        Loan loan = find(id);
        expect(loan, LoanStatus.PENDING);
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectedBy(staff);
        loan.setRejectReason(reason);
        return loans.save(loan);
    }

    /** The member handed the book in; a staff member still has to confirm the return. */
    public Loan requestReturn(String id, StaffRef staff) {
        Loan loan = find(id);
        expect(loan, LoanStatus.ACTIVE, LoanStatus.OVERDUE);
        loan.setStatus(LoanStatus.RETURN_PENDING);
        loan.setReturnRequestedBy(staff);
        loan.setReturnRequestedAt(Instant.now(clock));
        return loans.save(loan);
    }

    /** Confirms the return and puts the copy back into the catalog. */
    public Loan approveReturn(String id, StaffRef staff) {
        Loan loan = find(id);
        expect(loan, LoanStatus.ACTIVE, LoanStatus.OVERDUE, LoanStatus.RETURN_PENDING);
        remote.giveBack(loan.getBookId());
        if (loan.getReturnRequestedAt() == null) {
            loan.setReturnRequestedBy(staff);
            loan.setReturnRequestedAt(Instant.now(clock));
        }
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnApprovedBy(staff);
        loan.setReturnDate(LocalDate.now(clock));
        return loans.save(loan);
    }

    /** Cancels a return request, e.g. when the wrong book was handed in. */
    public Loan cancelReturn(String id) {
        Loan loan = find(id);
        expect(loan, LoanStatus.RETURN_PENDING);
        loan.setReturnRequestedAt(null);
        loan.setReturnRequestedBy(null);
        loan.setStatus(loan.getDueDate().isBefore(LocalDate.now(clock)) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE);
        return loans.save(loan);
    }

    public Loan extend(String id, int days) {
        Loan loan = find(id);
        expect(loan, LoanStatus.ACTIVE);
        if (loan.getExtensionCount() >= maxExtensions) {
            throw conflict("Bu ödünç en fazla " + maxExtensions + " kez uzatılabilir");
        }
        loan.setDueDate(loan.getDueDate().plusDays(days));
        loan.setExtensionCount(loan.getExtensionCount() + 1);
        return loans.save(loan);
    }

    public Loan markReminded(String id) {
        Loan loan = find(id);
        loan.setLastReminderAt(Instant.now(clock));
        loan.setReminderCount(loan.getReminderCount() + 1);
        return loans.save(loan);
    }

    /** Moves loans whose due date has passed to OVERDUE. Runs at start-up and every 15 minutes. */
    @Scheduled(initialDelay = 5_000, fixedDelayString = "${library.loan.overdue-check-ms:900000}")
    public int markOverdue() {
        List<Loan> late = loans.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now(clock));
        late.forEach(l -> l.setStatus(LoanStatus.OVERDUE));
        loans.saveAll(late);
        if (!late.isEmpty()) log.info("{} ödünç gecikmiş olarak işaretlendi", late.size());
        return late.size();
    }

    public Loan find(String id) {
        return loans.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ödünç kaydı bulunamadı"));
    }

    private static void expect(Loan loan, LoanStatus... allowed) {
        for (LoanStatus s : allowed) {
            if (loan.getStatus() == s) return;
        }
        throw conflict("Bu işlem '" + loan.getStatus() + "' durumundaki ödünç için yapılamaz");
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
