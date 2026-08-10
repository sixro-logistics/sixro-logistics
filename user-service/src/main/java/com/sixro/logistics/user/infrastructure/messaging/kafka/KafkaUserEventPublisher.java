package com.sixro.logistics.user.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 사용자 이벤트를 Kafka Topic으로 전송합니다.
     *
     * @param topic   발행할 Kafka Topic
     * @param key     메시지 Key
     * @param payload JSON Payload
     */
    public CompletableFuture<?> publish(
            String topic,
            String key,
            String payload
    ) {
        return kafkaTemplate.send(
                topic,
                key,
                payload
        );
    }
}