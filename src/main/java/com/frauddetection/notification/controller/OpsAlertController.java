package com.frauddetection.notification.controller;

import com.frauddetection.notification.model.dto.AlertSummaryDto;
import com.frauddetection.notification.model.dto.DashboardStatsDto;
import com.frauddetection.notification.model.entity.AuditLog;
import com.frauddetection.notification.model.enums.DecisionSource;
import com.frauddetection.notification.model.enums.FraudDecision;
import com.frauddetection.notification.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class OpsAlertController {

    private final AuditLogRepository auditLogRepository;

    /**
     * GET /api/v1/alerts
     * Returns all audit records — most recent first.
     */
    @GetMapping
    public ResponseEntity<List<AlertSummaryDto>> getAllAlerts() {
        List<AlertSummaryDto> alerts = auditLogRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getProcessedAt().compareTo(a.getProcessedAt()))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/v1/alerts/{id}
     * Returns a single audit record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertSummaryDto> getAlertById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/alerts/payment/{paymentRef}
     * Returns audit record by payment reference.
     */
    @GetMapping("/payment/{paymentRef}")
    public ResponseEntity<AlertSummaryDto> getAlertByPaymentRef(@PathVariable String paymentRef) {
        return auditLogRepository.findByPaymentRef(paymentRef)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/alerts/decision/{decision}
     * Filters alerts by decision: APPROVE, BLOCK, REVIEW
     */
    @GetMapping("/decision/{decision}")
    public ResponseEntity<List<AlertSummaryDto>> getAlertsByDecision(
            @PathVariable FraudDecision decision) {
        List<AlertSummaryDto> alerts = auditLogRepository
                .findByDecisionOrderByProcessedAtDesc(decision)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/v1/alerts/account/{accountNumber}
     * Returns all alerts for a given sender account.
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<AlertSummaryDto>> getAlertsByAccount(
            @PathVariable String accountNumber) {
        List<AlertSummaryDto> alerts = auditLogRepository
                .findBySenderAccountOrderByProcessedAtDesc(accountNumber)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/v1/alerts/stats
     * Returns aggregate stats for the ops dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalProcessed(auditLogRepository.countTotal())
                .totalApproved(auditLogRepository.countByDecision(FraudDecision.APPROVE))
                .totalBlocked(auditLogRepository.countByDecision(FraudDecision.BLOCK))
                .totalReview(auditLogRepository.countByDecision(FraudDecision.REVIEW))
                .decidedByRuleEngine(auditLogRepository.countByDecisionSource(DecisionSource.RULE_ENGINE))
                .decidedByAiAgent(auditLogRepository.countByDecisionSource(DecisionSource.AI_AGENT))
                .build();
        return ResponseEntity.ok(stats);
    }

    // ── Mapper ────────────────────────────────────────────

    private AlertSummaryDto toDto(AuditLog log) {
        return AlertSummaryDto.builder()
                .id(log.getId())
                .paymentId(log.getPaymentId())
                .paymentRef(log.getPaymentRef())
                .senderAccount(log.getSenderAccount())
                .receiverAccount(log.getReceiverAccount())
                .amount(log.getAmount())
                .currency(log.getCurrency())
                .decision(log.getDecision())
                .decisionSource(log.getDecisionSource())
                .fraudScore(log.getFraudScore())
                .decisionReason(log.getDecisionReason())
                .notificationStatus(log.getNotificationStatus())
                .decidedAt(log.getDecidedAt())
                .processedAt(log.getProcessedAt())
                .build();
    }
}