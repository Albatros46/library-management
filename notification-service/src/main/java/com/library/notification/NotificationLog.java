package com.library.notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("notification_logs")
public class NotificationLog {
    @Id
    private String id;
    @Indexed
    private String loanId;
    @Indexed
    private String memberId;
    private String recipient;
    private Type type;
    private String subject;
    private Instant sentAt;
    private boolean success;
    private String error;
    /** Staff member who triggered a manual reminder; null for scheduled ones. */
    private String triggeredBy;

    public enum Type {
        /** Due date is close. */
        DUE_SOON,
        /** Due date passed: return request sent automatically. */
        OVERDUE,
        /** Return request sent by a staff member from the overdue list. */
        MANUAL_REMINDER
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
}
