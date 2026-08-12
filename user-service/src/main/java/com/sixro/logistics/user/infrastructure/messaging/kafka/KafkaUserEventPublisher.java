package com.sixro.logistics.user.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Outbox에 저장된 사용자 이벤트를 Kafka로 전송하는 컴포넌트입니다.
 *
 * <p>UserCommandService에서 직접 호출하지 않으며,
 * Outbox 이벤트 발행 처리를 담당하는 컴포넌트에서 사용합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class KafkaUserEventPublisher {

    // Consumer가 중복 이벤트를 식별할 때 사용하는 Outbox 이벤트 ID입니다.

    public static final String EVENT_ID_HEADER = "event-id";

    // Kafka 메시지에 포함하는 사용자 이벤트 유형입니다.

    public static final String EVENT_TYPE_HEADER = "event-type";

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 사용자 이벤트를 Kafka Topic으로 전송합니다.
     *
     * <p>Kafka는 at-least-once 방식으로 동일 이벤트가 다시 전달될 수 있으므로
     * Consumer가 중복을 식별할 수 있도록 Outbox eventId를 Header에 포함합니다.</p>
     *
     * @param topic   발행할 Kafka Topic
     * @param key     메시지 Key
     * @param payload JSON Payload
     * @param eventId   Outbox 이벤트 식별자
     * @param eventType Outbox 이벤트 유형
     */
    public CompletableFuture<?> publish(
            String topic,
            String key,
            String payload,
            UUID eventId,
            String eventType
    ) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        topic,
                        key,
                        payload
                );

        record.headers().add(
                new RecordHeader(
                        EVENT_ID_HEADER,
                        eventId.toString()
                                .getBytes(StandardCharsets.UTF_8)
                )
        );

        record.headers().add(
                new RecordHeader(
                        EVENT_TYPE_HEADER,
                        eventType.getBytes(StandardCharsets.UTF_8)
                )
        );

        return kafkaTemplate.send(record);
    }
}