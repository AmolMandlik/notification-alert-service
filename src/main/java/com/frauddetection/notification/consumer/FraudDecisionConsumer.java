package com.frauddetection.notification.consumer;

import com.frauddetection.notification.model.dto.FraudDecisionEvent;
import com.frauddetection.notification.service.AlertProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionConsumer {

    private final AlertProcessingService alertProcessingService;

    /**
     * Consumes from payment-decisions topic.
     * <p>
     * Manual ack strategy:
     * - ack ONLY after full persist + notification pipeline completes
     * - notification failure does NOT block ack — it is logged and recorded in audit
     *   (notification is best-effort; we do not want Kafka retries flooding ops team with duplicate alerts)
     * - if persist itself fails — do NOT ack — Kafka retries the event
     */
    @KafkaListener(
            topics = "${app.kafka.topic.payment-decisions}",
            groupId = "notification-alert-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, FraudDecisionEvent> record, Acknowledgment ack) {
        FraudDecisionEvent event = record.value();

        log.info("[CONSUMER] Received paymentRef={} decision={} partition={} offset={}",
                event.getPaymentRef(), event.getDecision(), record.partition(), record.offset());

        try {
            alertProcessingService.process(event);
            ack.acknowledge();
            log.info("[CONSUMER] Acknowledged paymentRef={}", event.getPaymentRef());

        } catch (Exception e) {
            // Persist failed — do NOT ack, let Kafka retry
            log.error("[CONSUMER] Processing failed for paymentRef={} — NOT acknowledging. Error: {}",
                    event.getPaymentRef(), e.getMessage(), e);
            throw e;
        }
    }
}