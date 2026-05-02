package com.frauddetection.notification.service;

import com.frauddetection.notification.model.dto.FraudDecisionEvent;
import com.frauddetection.notification.model.entity.AuditLog;
import com.frauddetection.notification.model.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertProcessingService {

    private final AuditService auditService;
    private final NotificationService notificationService;

    /**
     * Full processing pipeline:
     * 1. Idempotency check — skip if already processed (Kafka redelivery guard)
     * 2. Persist audit record immediately
     * 3. Dispatch notifications
     * 4. Update audit record with notification outcome
     */
    public void process(FraudDecisionEvent event) {
        log.info("[PROCESSING] paymentRef={} decision={} source={} score={}",
                event.getPaymentRef(), event.getDecision(),
                event.getDecisionSource(), event.getFraudScore());

        // Step 1 — idempotency guard
        if (auditService.isAlreadyProcessed(event.getPaymentRef())) {
            log.warn("[PROCESSING] Duplicate event received for paymentRef={} — skipping",
                    event.getPaymentRef());
            return;
        }

        // Step 2 — persist audit record first (audit trail must exist regardless of notification outcome)
        AuditLog auditLog = auditService.persist(event);

        // Step 3 — dispatch notifications
        NotificationStatus notificationStatus = notificationService.dispatch(event);

        // Step 4 — update audit with notification result
        String errorDetail = notificationStatus == NotificationStatus.FAILED
                ? "Notification dispatch failed — check logs for details"
                : null;
        auditService.updateNotificationStatus(auditLog, notificationStatus, errorDetail);

        log.info("[PROCESSING] Complete paymentRef={} decision={} notificationStatus={}",
                event.getPaymentRef(), event.getDecision(), notificationStatus);
    }
}