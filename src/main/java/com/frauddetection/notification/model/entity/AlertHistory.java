package com.frauddetection.notification.model.entity;

import com.frauddetection.notification.model.enums.NotificationStatus;
import com.frauddetection.notification.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "payment_ref", nullable = false, length = 64)
    private String paymentRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "recipient", nullable = false, length = 128)
    private String recipient;           // email address or phone number

    @Column(name = "subject", length = 256)
    private String subject;

    @Column(name = "message", length = 2048)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationStatus status;

    @Column(name = "error_detail", length = 512)
    private String errorDetail;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}