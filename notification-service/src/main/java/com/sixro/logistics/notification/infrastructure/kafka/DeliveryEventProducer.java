package com.sixro.logistics.notification.infrastructure.kafka;

import com.sixro.logistics.notification.domain.event.DeliveryCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "delivery.events";

    // DB 트랜잭션 커밋 완료 직후 실행 (트랜잭션 롤백 시 실행 안 됨)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryCreatedEvent(DeliveryCreatedEvent event) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                TOPIC,
                event.getData().getDeliveryId().toString(), // Kafka 메시지 Key
                event
        );

        // Header 설정
        record.headers().add("event-type", "DeliveryCreatedEvent".getBytes(StandardCharsets.UTF_8));
        record.headers().add("trace-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Kafka Producer] Successfully sent DeliveryCreatedEvent. eventId: {}", event.getEventId());
            } else {
                log.error("[Kafka Producer] Failed to send DeliveryCreatedEvent. eventId: {}", event.getEventId(), ex);
            }
        });
    }
}
