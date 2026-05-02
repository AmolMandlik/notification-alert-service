package com.frauddetection.notification.model.dto;

import com.frauddetection.notification.model.enums.DecisionSource;
import com.frauddetection.notification.model.enums.FraudDecision;
import com.frauddetection.notification.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDto {

    private Long id;
    private Long paymentId;
    private String paymentRef;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal amount;
    private String currency;
    private FraudDecision decision;
    private DecisionSource decisionSource;
    private int fraudScore;
    private String decisionReason;
    private NotificationStatus notificationStatus;
    private LocalDateTime decidedAt;
    private LocalDateTime processedAt;
}