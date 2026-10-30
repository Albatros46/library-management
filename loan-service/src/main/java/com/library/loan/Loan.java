package com.library.loan;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One borrowing of one book copy by one member.
 * Member and book fields are snapshots taken when the loan is requested, so history and
 * reading reports stay readable even if the catalog changes later.
 */
@Document("loans")
public class Loan {
    @Id
    private String id;

    @Indexed
    private String memberId;
    private String memberName;
    private String memberEmail;

    @Indexed
    private String bookId;
    private String bookTitle;
    private String authorId;
    private String authorName;
    private String category;
    private int pageCount;

    @Indexed
    private LoanStatus status;

    private StaffRef requestedBy;
    private Instant requestedAt;
    private StaffRef approvedBy;
    private Instant approvedAt;
    private LocalDate loanDate;
    @Indexed
    private LocalDate dueDate;
    private int extensionCount;

    private StaffRef returnRequestedBy;
    private Instant returnRequestedAt;
    private StaffRef returnApprovedBy;
    private LocalDate returnDate;

    private StaffRef rejectedBy;
    private String rejectReason;

    private Instant lastReminderAt;
    private int reminderCount;
    private String note;

    public record StaffRef(String id, String name) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public StaffRef getRequestedBy() { return requestedBy; }
    public void setRequestedBy(StaffRef requestedBy) { this.requestedBy = requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public StaffRef getApprovedBy() { return approvedBy; }
    public void setApprovedBy(StaffRef approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getExtensionCount() { return extensionCount; }
    public void setExtensionCount(int extensionCount) { this.extensionCount = extensionCount; }
    public StaffRef getReturnRequestedBy() { return returnRequestedBy; }
    public void setReturnRequestedBy(StaffRef returnRequestedBy) { this.returnRequestedBy = returnRequestedBy; }
    public Instant getReturnRequestedAt() { return returnRequestedAt; }
    public void setReturnRequestedAt(Instant returnRequestedAt) { this.returnRequestedAt = returnRequestedAt; }
    public StaffRef getReturnApprovedBy() { return returnApprovedBy; }
    public void setReturnApprovedBy(StaffRef returnApprovedBy) { this.returnApprovedBy = returnApprovedBy; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public StaffRef getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(StaffRef rejectedBy) { this.rejectedBy = rejectedBy; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Instant getLastReminderAt() { return lastReminderAt; }
    public void setLastReminderAt(Instant lastReminderAt) { this.lastReminderAt = lastReminderAt; }
    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
