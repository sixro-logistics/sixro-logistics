package com.sixro.logistics.notification.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // 3회 재시도 실패 시 delivery.events.dlq 토픽으로 전송하는 Recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.error("[Kafka DLQ] Retries exhausted. Moving message to DLQ. Key: {}", record.key(), ex);
                    return new org.apache.kafka.common.TopicPartition("delivery.events.dlq", record.partition());
                });

        // 1초 간격으로 최대 3회 재시도 (초기 시도 1회 + 재시도 2회 = 총 3회 execution)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
