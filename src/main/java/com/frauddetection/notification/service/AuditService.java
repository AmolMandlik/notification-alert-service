package com.frauddetection.notification.service;

import com.frauddetection.notification.model.dto.FraudDecisionEvent;
import com.frauddetection.notification.model.entity.AuditLog;
import com.frauddetection.notification.model.enums.NotificationStatus;
import com.frauddetection.notification.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Returns true if this paymentRef was already processed.
     * Must be called BEFORE any processing to guard against Kafka redelivery.
     */
    public boolean isAlreadyProcessed(String paymentRef) {
        return auditLogRepository.existsByPaymentRef(paymentRef);
    }

    /**
     * Persists audit record immediately on receipt — before notifications.
     * This ensures we always have a record even if notification dispatch fails.
     */
    @Transactional
    public AuditLog persist(FraudDecisionEvent event) {
        AuditLog auditLoglog = AuditLog.builder()
                .paymentId(event.getPaymentId())
                .paymentRef(event.getPaymentRef())
                .senderAccount(event.getSenderAccount())
                .receiverAccount(event.getReceiverAccount())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .decision(event.getDecision())
                .decisionSource(event.getDecisionSource())
                .fraudScore(event.getFraudScore())
                .decisionReason(event.getDecisionReason())
                .decidedAt(event.getDecidedAt())
                .notificationStatus(NotificationStatus.SKIPPED) // default — updated after dispatch
                .build();

        AuditLog saved = auditLogRepository.save(auditLoglog);
        log.info("[AUDIT] Persisted paymentRef={} decision={} score={}",
                event.getPaymentRef(), event.getDecision(), event.getFraudScore());
        return saved;
    }

    /**
     * Updates the notification outcome on an existing audit record.
     */
    @Transactional
    public void updateNotificationStatus(AuditLog auditLog,
                                         NotificationStatus status,
                                         String errorDetail) {
        auditLog.setNotificationStatus(status);
        auditLog.setNotificationError(errorDetail);
        auditLogRepository.save(auditLog);
    }
}