package com.library.notification;

import com.library.notification.NotificationLog.Type;
import com.library.notification.RemoteClients.LoanDto;
import com.library.notification.RemoteClients.MemberDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RemoteClients remote;
    private final NotificationLogRepository logs;
    private final JavaMailSender mail;
    private final String from;
    private final String libraryName;
    private final int dueSoonDays;
    private final Duration overdueRepeat;
    private final ZoneId zone;

    public NotificationService(RemoteClients remote, NotificationLogRepository logs, JavaMailSender mail,
                               @Value("${library.mail.from}") String from,
                               @Value("${library.mail.library-name}") String libraryName,
                               @Value("${library.notify.due-soon-days:2}") int dueSoonDays,
                               @Value("${library.notify.overdue-repeat:P3D}") Duration overdueRepeat,
                               @Value("${library.time-zone:Europe/Istanbul}") String zone) {
        this.remote = remote;
        this.logs = logs;
        this.mail = mail;
        this.from = from;
        this.libraryName = libraryName;
        this.dueSoonDays = dueSoonDays;
        this.overdueRepeat = overdueRepeat;
        this.zone = ZoneId.of(zone);
    }

    public record RunResult(int dueSoonSent, int overdueSent, int failed) {}

    /** Daily job: warn members whose due date is near, and ask overdue members to return their books. */
    @Scheduled(cron = "${library.notify.cron:0 0 9 * * *}", zone = "${library.time-zone:Europe/Istanbul}")
    public void scheduled() {
        runNow();
    }

    public RunResult runNow() {
        int dueSoon = 0, overdue = 0, failed = 0;

        for (LoanDto loan : remote.dueSoon(dueSoonDays)) {
            if (logs.existsByLoanIdAndTypeAndSuccessTrue(loan.id(), Type.DUE_SOON)) continue;
            MemberDto member = memberOrNull(loan.memberId());
            if (member != null && !member.emailNotifications()) continue;
            if (send(loan, member, Type.DUE_SOON, null).isSuccess()) dueSoon++; else failed++;
        }

        Instant repeatAfter = Instant.now().minus(overdueRepeat);
        for (LoanDto loan : remote.overdueLoans()) {
            if (logs.existsByLoanIdAndTypeInAndSuccessTrueAndSentAtAfter(
                    loan.id(), List.of(Type.OVERDUE, Type.MANUAL_REMINDER), repeatAfter)) continue;
            // Overdue return requests go out even if the member opted out of reminders.
            if (send(loan, memberOrNull(loan.memberId()), Type.OVERDUE, null).isSuccess()) overdue++; else failed++;
        }

        log.info("Bildirim çalışması: {} yaklaşan, {} gecikme, {} hatalı", dueSoon, overdue, failed);
        return new RunResult(dueSoon, overdue, failed);
    }

    /** Return request sent by a staff member from the overdue list. */
    public NotificationLog sendManualReminder(String loanId, String staffName) {
        LoanDto loan = remote.loan(loanId);
        return send(loan, memberOrNull(loan.memberId()), Type.MANUAL_REMINDER, staffName);
    }

    private NotificationLog send(LoanDto loan, MemberDto member, Type type, String staffName) {
        String to = member != null && member.email() != null ? member.email() : loan.memberEmail();
        String name = member != null ? member.firstName() + " " + member.lastName() : loan.memberName();

        NotificationLog entry = new NotificationLog();
        entry.setLoanId(loan.id());
        entry.setMemberId(loan.memberId());
        entry.setRecipient(to);
        entry.setType(type);
        entry.setTriggeredBy(staffName);
        entry.setSentAt(Instant.now());

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject(loan, type));
        msg.setText(body(loan, name, type));
        entry.setSubject(msg.getSubject());
        try {
            mail.send(msg);
            entry.setSuccess(true);
            if (type != Type.DUE_SOON) remote.markReminded(loan.id());
        } catch (Exception e) {
            log.warn("E-posta gönderilemedi ({} -> {}): {}", type, to, e.getMessage());
            entry.setSuccess(false);
            entry.setError(e.getMessage());
        }
        return logs.save(entry);
    }

    private String subject(LoanDto loan, Type type) {
        return switch (type) {
            case DUE_SOON -> libraryName + " – İade tarihi yaklaşıyor: " + loan.bookTitle();
            case OVERDUE, MANUAL_REMINDER -> libraryName + " – İade süresi geçti: " + loan.bookTitle();
        };
    }

    private String body(LoanDto loan, String name, Type type) {
        String book = "\"" + loan.bookTitle() + "\"" + (loan.authorName() != null ? " (" + loan.authorName() + ")" : "");
        String due = loan.dueDate().format(DATE);
        if (type == Type.DUE_SOON) {
            return "Merhaba " + name + ",\n\n"
                    + "Ödünç aldığınız " + book + " adlı kitabın iade tarihi " + due + ".\n"
                    + "Kitabı bu tarihe kadar kütüphanemize teslim etmenizi rica ederiz.\n\n"
                    + "Teşekkürler,\n" + libraryName;
        }
        long late = ChronoUnit.DAYS.between(loan.dueDate(), LocalDate.now(zone));
        return "Merhaba " + name + ",\n\n"
                + "Ödünç aldığınız " + book + " adlı kitabın iade tarihi " + due + " idi ve "
                + late + " gün gecikmiştir.\n"
                + "Lütfen kitabı en kısa sürede kütüphanemize iade ediniz. Gecikmeli kitabı olan üyelere "
                + "iade yapılana kadar yeni ödünç verilmemektedir.\n\n"
                + "Teşekkürler,\n" + libraryName;
    }

    private MemberDto memberOrNull(String id) {
        try {
            return remote.member(id);
        } catch (Exception e) {
            log.warn("Üye bilgisi alınamadı ({}), ödünçteki e-posta kullanılacak: {}", id, e.getMessage());
            return null;
        }
    }
}
