package com.library.notification;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    List<NotificationLog> findTop200ByOrderBySentAtDesc();
    List<NotificationLog> findByLoanIdOrderBySentAtDesc(String loanId);
    List<NotificationLog> findByMemberIdOrderBySentAtDesc(String memberId);
    List<NotificationLog> findByLoanIdInOrderBySentAtDesc(List<String> loanIds);
    boolean existsByLoanIdAndTypeAndSuccessTrue(String loanId, NotificationLog.Type type);
    boolean existsByLoanIdAndTypeInAndSuccessTrueAndSentAtAfter(String loanId, List<NotificationLog.Type> types, Instant after);
}
