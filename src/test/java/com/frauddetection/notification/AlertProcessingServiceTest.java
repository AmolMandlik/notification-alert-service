package com.frauddetection.notification;

import com.frauddetection.notification.model.dto.FraudDecisionEvent;
import com.frauddetection.notification.model.entity.AuditLog;
import com.frauddetection.notification.model.enums.DecisionSource;
import com.frauddetection.notification.model.enums.FraudDecision;
import com.frauddetection.notification.model.enums.NotificationStatus;
import com.frauddetection.notification.service.AlertProcessingService;
import com.frauddetection.notification.service.AuditService;
import com.frauddetection.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertProcessingServiceTest {

    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AlertProcessingService alertProcessingService;

    private FraudDecisionEvent approveEvent;
    private FraudDecisionEvent blockEvent;
    private FraudDecisionEvent reviewEvent;
    private AuditLog mockAuditLog;

    @BeforeEach
    void setup() {
        approveEvent = buildEvent("REF-APPROVE-001", FraudDecision.APPROVE, 15);
        blockEvent   = buildEvent("REF-BLOCK-001",   FraudDecision.BLOCK,   85);
        reviewEvent  = buildEvent("REF-REVIEW-001",  FraudDecision.REVIEW,  50);

        mockAuditLog = AuditLog.builder()
                .id(1L)
                .paymentRef("REF-TEST")
                .build();
    }

    @Test
    void shouldProcessApproveEventSuccessfully() {
        when(auditService.isAlreadyProcessed(approveEvent.getPaymentRef())).thenReturn(false);
        when(auditService.persist(approveEvent)).thenReturn(mockAuditLog);
        when(notificationService.dispatch(approveEvent)).thenReturn(NotificationStatus.SENT);

        alertProcessingService.process(approveEvent);

        verify(auditService).persist(approveEvent);
        verify(notificationService).dispatch(approveEvent);
        verify(auditService).updateNotificationStatus(mockAuditLog, NotificationStatus.SENT, null);
    }

    @Test
    void shouldProcessBlockEventSuccessfully() {
        when(auditService.isAlreadyProcessed(blockEvent.getPaymentRef())).thenReturn(false);
        when(auditService.persist(blockEvent)).thenReturn(mockAuditLog);
        when(notificationService.dispatch(blockEvent)).thenReturn(NotificationStatus.SENT);

        alertProcessingService.process(blockEvent);

        verify(auditService).persist(blockEvent);
        verify(notificationService).dispatch(blockEvent);
        verify(auditService).updateNotificationStatus(mockAuditLog, NotificationStatus.SENT, null);
    }

    @Test
    void shouldSkipDuplicateEvent() {
        when(auditService.isAlreadyProcessed(approveEvent.getPaymentRef())).thenReturn(true);

        alertProcessingService.process(approveEvent);

        verify(auditService, never()).persist(any());
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void shouldUpdateAuditWithFailedStatusWhenNotificationFails() {
        when(auditService.isAlreadyProcessed(blockEvent.getPaymentRef())).thenReturn(false);
        when(auditService.persist(blockEvent)).thenReturn(mockAuditLog);
        when(notificationService.dispatch(blockEvent)).thenReturn(NotificationStatus.FAILED);

        alertProcessingService.process(blockEvent);

        verify(auditService).updateNotificationStatus(
                eq(mockAuditLog),
                eq(NotificationStatus.FAILED),
                any(String.class));
    }

    @Test
    void shouldProcessReviewEventAndAlertOpsTeam() {
        when(auditService.isAlreadyProcessed(reviewEvent.getPaymentRef())).thenReturn(false);
        when(auditService.persist(reviewEvent)).thenReturn(mockAuditLog);
        when(notificationService.dispatch(reviewEvent)).thenReturn(NotificationStatus.SENT);

        alertProcessingService.process(reviewEvent);

        verify(auditService).persist(reviewEvent);
        verify(notificationService).dispatch(reviewEvent);
    }

    // ── Helper ────────────────────────────────────────────

    private FraudDecisionEvent buildEvent(String paymentRef, FraudDecision decision, int score) {
        return FraudDecisionEvent.builder()
                .paymentId(12345L)
                .paymentRef(paymentRef)
                .senderAccount("SENDER123456")
                .receiverAccount("RECEIVER654321")
                .amount(new BigDecimal("50000.00"))
                .currency("INR")
                .decision(decision)
                .decisionSource(DecisionSource.RULE_ENGINE)
                .fraudScore(score)
                .decisionReason("Test reason for " + decision)
                .decidedAt(LocalDateTime.now())
                .build();
    }
}