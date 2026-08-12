package com.sixro.logistics.delivery.infrastructure.kafka.config;

import com.sixro.logistics.delivery.infrastructure.kafka.consumer.DeliveryCreationFailureRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerErrorConfig {

    private static final long RETRY_SPACE = 2000L;
    private static final long RETRY_COUNT = 3L;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeliveryCreationFailureRecoverer recoverer) {
        // Kafka Consumer 재시도 정책
        FixedBackOff fixedBackOff = new FixedBackOff(RETRY_SPACE, RETRY_COUNT);

        // 재시도 소진 후 실패 복구 처리
        return new DefaultErrorHandler(recoverer, fixedBackOff);
    }
}
