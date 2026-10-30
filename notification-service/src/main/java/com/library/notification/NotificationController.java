package com.library.notification;

import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final NotificationLogRepository logs;

    public NotificationController(NotificationService service, NotificationLogRepository logs) {
        this.service = service;
        this.logs = logs;
    }

    @GetMapping
    public List<NotificationLog> list(@RequestParam(required = false) String loanId,
                                      @RequestParam(required = false) String memberId,
                                      @RequestParam(required = false) List<String> loanIds) {
        if (loanId != null) return logs.findByLoanIdOrderBySentAtDesc(loanId);
        if (memberId != null) return logs.findByMemberIdOrderBySentAtDesc(memberId);
        if (loanIds != null) return logs.findByLoanIdInOrderBySentAtDesc(loanIds);
        return logs.findTop200ByOrderBySentAtDesc();
    }

    /** Sends a return request e-mail for one overdue loan right now. */
    @PostMapping("/loans/{loanId}/reminder")
    public NotificationLog remind(@PathVariable String loanId,
                                  @RequestHeader(value = "X-User-Name", required = false) String userName) {
        String staff = userName == null ? null : URLDecoder.decode(userName, StandardCharsets.UTF_8);
        return service.sendManualReminder(loanId, staff);
    }

    /** Runs the daily due-soon / overdue job immediately. */
    @PostMapping("/run")
    public NotificationService.RunResult run() {
        return service.runNow();
    }
}
