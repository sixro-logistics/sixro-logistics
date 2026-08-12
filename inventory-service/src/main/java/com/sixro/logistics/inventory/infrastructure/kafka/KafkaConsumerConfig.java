package com.sixro.logistics.inventory.infrastructure.kafka;

import com.sixro.logistics.common.core.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    /**
     * DLT 전용 KafkaTemplate
     * 원본 이벤트가 String(JSON)이므로 StringSerializer를 사용
     */
    @Bean
    public KafkaTemplate<String, String> dltKafkaTemplate(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props =
                kafkaProperties.buildProducerProperties();

        ProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(
                        props,
                        new StringSerializer(),
                        new StringSerializer()
                );

        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Consumer 처리 실패 시 DLT로 메시지를 발행
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> dltKafkaTemplate
    ) {
        return new DeadLetterPublishingRecoverer(dltKafkaTemplate);
    }

    /**
     * Consumer 예외 처리
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer
    ) {
        FixedBackOff fixedBackOff = new FixedBackOff(
                1_000L, // 1초 간격
                2L      // Retry 2회 → 최초 처리 포함 총 3회 시도
        );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        fixedBackOff
                );

        // 재시도해도 해결될 가능성이 없는 예외
        errorHandler.addNotRetryableExceptions(
                BaseException.class,
                EventDeserializationException.class
        );

        return errorHandler;
    }

    /**
     * @KafkaListener에 ErrorHandler를 연결
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }
}