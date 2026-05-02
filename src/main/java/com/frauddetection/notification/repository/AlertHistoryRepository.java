package com.frauddetection.notification.repository;

import com.frauddetection.notification.model.entity.AlertHistory;
import com.frauddetection.notification.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    List<AlertHistory> findByPaymentRefOrderBySentAtDesc(String paymentRef);

    List<AlertHistory> findByNotificationTypeOrderBySentAtDesc(NotificationType type);
}
