package com.frauddetection.notification.service;

import com.frauddetection.notification.config.NotificationProperties;
import com.frauddetection.notification.model.dto.FraudDecisionEvent;
import com.frauddetection.notification.model.entity.AlertHistory;
import com.frauddetection.notification.model.enums.FraudDecision;
import com.frauddetection.notification.model.enums.NotificationStatus;
import com.frauddetection.notification.model.enums.NotificationType;
import com.frauddetection.notification.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final NotificationProperties notificationProperties;

    /**
     * Routes notification based on decision type.
     * APPROVE  → customer informed, no ops alert needed
     * BLOCK    → customer informed + ops team alerted
     * REVIEW   → ops team alerted only (customer not informed until manual review)
     * <p>
     * Returns the final NotificationStatus to update the audit log.
     */
    @Transactional
    public NotificationStatus dispatch(FraudDecisionEvent event) {
        FraudDecision decision = event.getDecision();
        log.info("[NOTIFICATION] Dispatching for paymentRef={} decision={}", event.getPaymentRef(), decision);

        try {
            switch (decision) {
                case APPROVE -> dispatchApproval(event);
                case BLOCK   -> {
                    dispatchBlock(event);
                    dispatchOpsBlockAlert(event);
                }
                case REVIEW  -> dispatchOpsReviewAlert(event);
            }
            return NotificationStatus.SENT;

        } catch (Exception e) {
            log.error("[NOTIFICATION] Dispatch failed for paymentRef={} error={}", event.getPaymentRef(), e.getMessage(), e);
            return NotificationStatus.FAILED;
        }
    }

    // ── Notification builders ─────────────────────────────

    private void dispatchApproval(FraudDecisionEvent event) {
        String recipient = buildCustomerEmail(event.getSenderAccount());
        String subject   = "Payment Approved — Ref: " + event.getPaymentRef();
        String message   = String.format(
                "Dear Customer,%n%n" +
                "Your payment of %s %s to account %s has been approved and is being processed.%n%n" +
                "Payment Reference: %s%n" +
                "Fraud Score: %d/100 (Low Risk)%n%n" +
                "Thank you for banking with us.",
                event.getCurrency(), formatAmount(event.getAmount()),
                event.getReceiverAccount(), event.getPaymentRef(), event.getFraudScore());

        sendAndRecord(event.getPaymentRef(), NotificationType.CUSTOMER_APPROVAL, recipient, subject, message);
    }

    private void dispatchBlock(FraudDecisionEvent event) {
        String recipient = buildCustomerEmail(event.getSenderAccount());
        String subject   = "Payment Blocked — Ref: " + event.getPaymentRef();
        String message   = String.format(
                "Dear Customer,%n%n" +
                "Your payment of %s %s to account %s has been blocked due to suspicious activity.%n%n" +
                "Payment Reference: %s%n" +
                "If you believe this is an error, please contact our fraud helpline immediately.%n%n" +
                "Thank you for your understanding.",
                event.getCurrency(), formatAmount(event.getAmount()),
                event.getReceiverAccount(), event.getPaymentRef());

        sendAndRecord(event.getPaymentRef(), NotificationType.CUSTOMER_BLOCK, recipient, subject, message);
    }

    private void dispatchOpsBlockAlert(FraudDecisionEvent event) {
        String subject = "[FRAUD ALERT — BLOCK] PaymentRef: " + event.getPaymentRef();
        String message = String.format(
                "A payment has been automatically BLOCKED by the fraud detection engine.%n%n" +
                "Payment Reference : %s%n" +
                "Sender Account    : %s%n" +
                "Receiver Account  : %s%n" +
                "Amount            : %s %s%n" +
                "Fraud Score       : %d/100%n" +
                "Decision Source   : %s%n" +
                "Reason            : %s%n%n" +
                "Please review in the fraud operations dashboard.",
                event.getPaymentRef(), event.getSenderAccount(), event.getReceiverAccount(),
                event.getCurrency(), formatAmount(event.getAmount()),
                event.getFraudScore(), event.getDecisionSource(), event.getDecisionReason());

        sendAndRecord(event.getPaymentRef(), NotificationType.OPS_BLOCK_ALERT,
                notificationProperties.getOpsEmail(), subject, message);
    }

    private void dispatchOpsReviewAlert(FraudDecisionEvent event) {
        String subject = "[FRAUD REVIEW REQUIRED] PaymentRef: " + event.getPaymentRef();
        String message = String.format(
                "A payment requires manual review by the fraud operations team.%n%n" +
                "Payment Reference : %s%n" +
                "Sender Account    : %s%n" +
                "Receiver Account  : %s%n" +
                "Amount            : %s %s%n" +
                "Fraud Score       : %d/100 (Borderline)%n" +
                "Decision Source   : %s%n" +
                "AI Agent Reason   : %s%n%n" +
                "Action required: Approve or reject this payment in the ops dashboard.",
                event.getPaymentRef(), event.getSenderAccount(), event.getReceiverAccount(),
                event.getCurrency(), formatAmount(event.getAmount()),
                event.getFraudScore(), event.getDecisionSource(), event.getDecisionReason());

        sendAndRecord(event.getPaymentRef(), NotificationType.OPS_REVIEW_ALERT,
                notificationProperties.getOpsEmail(), subject, message);
    }

    // ── Internal dispatch ─────────────────────────────────

    private void sendAndRecord(String paymentRef, NotificationType type,
                               String recipient, String subject, String message) {
        if (notificationProperties.isSimulate()) {
            // POC simulation — log instead of sending real email/SMS
            log.info("[NOTIFY] [SIMULATED] type={} to={} subject={}", type, recipient, subject);
            log.debug("[NOTIFY] [SIMULATED] message={}", message);
        } else {
            // TODO: wire real email/SMS gateway here (SendGrid, AWS SES, Twilio)
            log.info("[NOTIFY] Sending type={} to={}", type, recipient);
        }

        AlertHistory history = AlertHistory.builder()
                .paymentRef(paymentRef)
                .notificationType(type)
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .status(NotificationStatus.SENT)
                .build();

        alertHistoryRepository.save(history);
        log.info("[NOTIFY] Recorded alert paymentRef={} type={} recipient={}", paymentRef, type, recipient);
    }

    // ── Helpers ───────────────────────────────────────────

    private String buildCustomerEmail(String accountNumber) {
        // Simulated customer email derived from account number
        return accountNumber.toLowerCase() + notificationProperties.getCustomerEmailDomain();
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "N/A";
    }
}