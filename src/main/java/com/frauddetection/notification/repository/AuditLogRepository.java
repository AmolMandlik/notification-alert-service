package com.frauddetection.notification.repository;

import com.frauddetection.notification.model.entity.AuditLog;
import com.frauddetection.notification.model.enums.DecisionSource;
import com.frauddetection.notification.model.enums.FraudDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Idempotency guard — check before processing
    boolean existsByPaymentRef(String paymentRef);

    Optional<AuditLog> findByPaymentRef(String paymentRef);

    List<AuditLog> findByDecisionOrderByProcessedAtDesc(FraudDecision decision);

    List<AuditLog> findBySenderAccountOrderByProcessedAtDesc(String senderAccount);

    // Aggregate counts for dashboard stats
    long countByDecision(FraudDecision decision);

    long countByDecisionSource(DecisionSource decisionSource);

    @Query("SELECT COUNT(a) FROM AuditLog a")
    long countTotal();
}
