package com.library.loan;

import com.library.loan.Loan.StaffRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;
    private final LoanRepository loans;
    private final Clock clock;

    public LoanController(LoanService service, LoanRepository loans, Clock clock) {
        this.service = service;
        this.loans = loans;
        this.clock = clock;
    }

    public record LoanRequest(@NotBlank String memberId, @NotBlank String bookId,
                              @Min(1) @Max(90) Integer days, String note) {}

    public record RejectRequest(String reason) {}

    // ---- queries -------------------------------------------------------------------------

    @GetMapping
    public List<Loan> list(@RequestParam(required = false) String memberId,
                           @RequestParam(required = false) List<LoanStatus> status) {
        if (memberId != null) {
            return status == null
                    ? loans.findByMemberIdOrderByRequestedAtDesc(memberId)
                    : loans.findByMemberIdAndStatusInOrderByDueDate(memberId, status);
        }
        return loans.findByStatusInOrderByDueDate(status == null ? EnumSet.allOf(LoanStatus.class) : status);
    }

    @GetMapping("/{id}")
    public Loan get(@PathVariable String id) {
        return service.find(id);
    }

    /** Lending and return requests waiting for staff approval. */
    @GetMapping("/pending")
    public List<Loan> pending() {
        return loans.findByStatusInOrderByRequestedAt(EnumSet.of(LoanStatus.PENDING, LoanStatus.RETURN_PENDING));
    }

    /** Books currently with members, optionally for one member. */
    @GetMapping("/active")
    public List<Loan> active(@RequestParam(required = false) String memberId) {
        return memberId == null
                ? loans.findByStatusInOrderByDueDate(LoanStatus.ON_LOAN)
                : loans.findByMemberIdAndStatusInOrderByDueDate(memberId, LoanStatus.ON_LOAN);
    }

    /** The separate list of loans whose due date has passed. */
    @GetMapping("/overdue")
    public List<Loan> overdue() {
        service.markOverdue();
        return loans.findByStatusOrderByDueDate(LoanStatus.OVERDUE);
    }

    /** Active loans that become due within the next {@code days} days. */
    @GetMapping("/due-soon")
    public List<Loan> dueSoon(@RequestParam(defaultValue = "2") int days) {
        LocalDate today = LocalDate.now(clock);
        // Between is exclusive in Spring Data Mongo, so widen by one day on each side.
        return loans.findByStatusAndDueDateBetweenOrderByDueDate(LoanStatus.ACTIVE, today.minusDays(1), today.plusDays(days + 1L));
    }

    /** Who currently holds copies of a book. */
    @GetMapping("/books/{bookId}/active")
    public List<Loan> activeForBook(@PathVariable String bookId) {
        return loans.findByBookIdAndStatusIn(bookId, LoanStatus.ON_LOAN);
    }

    /** How many books, how many pages and in which categories a member borrowed. */
    @GetMapping("/members/{memberId}/stats")
    public MemberStats stats(@PathVariable String memberId) {
        return MemberStats.of(memberId, loans.findByMemberIdOrderByRequestedAtDesc(memberId));
    }

    // ---- workflow ------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Loan create(@Valid @RequestBody LoanRequest req,
                       @RequestHeader("X-User-Id") String userId,
                       @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return service.request(req.memberId(), req.bookId(), req.days(), req.note(), staff(userId, userName));
    }

    @PostMapping("/{id}/approve")
    public Loan approve(@PathVariable String id, @RequestHeader("X-User-Id") String userId,
                        @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return service.approve(id, staff(userId, userName));
    }

    @PostMapping("/{id}/reject")
    public Loan reject(@PathVariable String id, @RequestBody(required = false) RejectRequest req,
                       @RequestHeader("X-User-Id") String userId,
                       @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return service.reject(id, req == null ? null : req.reason(), staff(userId, userName));
    }

    @PostMapping("/{id}/return-request")
    public Loan requestReturn(@PathVariable String id, @RequestHeader("X-User-Id") String userId,
                              @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return service.requestReturn(id, staff(userId, userName));
    }

    @PostMapping("/{id}/return-approve")
    public Loan approveReturn(@PathVariable String id, @RequestHeader("X-User-Id") String userId,
                              @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return service.approveReturn(id, staff(userId, userName));
    }

    @PostMapping("/{id}/return-cancel")
    public Loan cancelReturn(@PathVariable String id) {
        return service.cancelReturn(id);
    }

    @PostMapping("/{id}/extend")
    public Loan extend(@PathVariable String id, @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
        return service.extend(id, days);
    }

    /** Internal: notification-service records that a reminder e-mail went out. Blocked at the gateway. */
    @PostMapping("/{id}/reminded")
    public Loan reminded(@PathVariable String id) {
        return service.markReminded(id);
    }

    private static StaffRef staff(String id, String encodedName) {
        // The gateway URL-encodes the name so non-ASCII characters survive the HTTP header.
        String name = encodedName == null ? id : URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        return new StaffRef(id, name);
    }
}
