package com.frauddetection.notification.model.entity;

import com.frauddetection.notification.model.enums.DecisionSource;
import com.frauddetection.notification.model.enums.FraudDecision;
import com.frauddetection.notification.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    // ── Payment reference ────────────────────────────────
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "payment_ref", nullable = false, unique = true, length = 64)
    private String paymentRef;           // unique — idempotency guard

    @Column(name = "sender_account", nullable = false, length = 32)
    private String senderAccount;

    @Column(name = "receiver_account", nullable = false, length = 32)
    private String receiverAccount;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    // ── Fraud decision ───────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private FraudDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_source", nullable = false, length = 20)
    private DecisionSource decisionSource;

    @Column(name = "fraud_score", nullable = false)
    private int fraudScore;

    @Column(name = "decision_reason", length = 2048)
    private String decisionReason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // ── Notification outcome ─────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 10)
    private NotificationStatus notificationStatus;

    @Column(name = "notification_error", length = 512)
    private String notificationError;   // populated only on FAILED status

    // ── Processing timestamp ─────────────────────────────
    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;
}