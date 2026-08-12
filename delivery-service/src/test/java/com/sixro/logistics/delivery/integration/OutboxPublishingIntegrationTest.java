package com.sixro.logistics.delivery.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.application.service.OutboxService;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.outbox.OutboxPublisher;
import com.sixro.logistics.delivery.infrastructure.repository.OutboxRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        KafkaTestContainerConfig.class
})
class OutboxPublishingIntegrationTest {

    private static final String TRACE_ID = "outbox-publishing-integration-trace-id";
    private static final UUID ORDER_ID = UUID.fromString("13000000-0000-0000-0000-000000000001");
    private static final UUID DELIVERY_ID = UUID.fromString("13000000-0000-0000-0000-000000000002");

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor scheduledTaskProcessor;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @BeforeEach
    void stopAutomaticOutboxPublishing() {
        // 테스트가 직접 publish()를 호출할 수 있도록 예약 실행과의 경쟁을 제거합니다.
        scheduledTaskProcessor.getScheduledTasks().forEach(task -> task.cancel(false));
    }

    @Test
    @DisplayName("PENDING Outbox를 delivery.created로 발행하고 PUBLISHED 상태로 변경한다")
    void publish_pendingOutbox_sendsKafkaEventAndMarksPublished() throws Exception {
        // given
        DeliveryCreatedEvent event = createDeliveryCreatedEvent();
        outboxService.save(event, TRACE_ID);

        Outbox pendingOutbox = outboxRepository.findById(event.eventId()).orElseThrow();
        assertThat(pendingOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(pendingOutbox.getPublishedAt()).isNull();

        kafkaAdmin.createOrModifyTopics(new NewTopic(KafkaTopics.DELIVERY_CREATED, 1, (short) 1));

        try (Consumer<String, String> consumer = createKafkaConsumer()) {
            consumer.subscribe(List.of(KafkaTopics.DELIVERY_CREATED));
            waitForPartitionAssignment(consumer);

            // when
            outboxPublisher.publish();
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                    consumer, KafkaTopics.DELIVERY_CREATED, Duration.ofSeconds(10));

            // then
            assertThat(record.key()).isEqualTo(DELIVERY_ID.toString());
            assertThat(headerValue(record, "event-type")).isEqualTo("DeliveryCreatedEvent");
            assertThat(headerValue(record, "trace-id")).isEqualTo(TRACE_ID);

            JsonNode publishedEvent = objectMapper.readTree(record.value());
            assertThat(publishedEvent.path("eventId").asText()).isEqualTo(event.eventId().toString());
            assertThat(publishedEvent.path("data").path("orderId").asText()).isEqualTo(ORDER_ID.toString());
            assertThat(publishedEvent.path("data").path("deliveryId").asText())
                    .isEqualTo(DELIVERY_ID.toString());
        }

        // Kafka 발행 성공 이후 Outbox 상태가 같은 트랜잭션에서 변경됐는지 확인
        Outbox publishedOutbox = outboxRepository.findById(event.eventId()).orElseThrow();
        assertThat(publishedOutbox.getEventType()).isEqualTo(OutboxEventType.DELIVERY_CREATED);
        assertThat(publishedOutbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedOutbox.getPublishedAt()).isNotNull();
    }

    private Consumer<String, String> createKafkaConsumer() {
        Map<String, Object> consumerProperties = new HashMap<>(KafkaTestUtils.consumerProps(
                kafkaConnectionDetails.getBootstrapServers().getFirst(),
                "outbox-publishing-integration-" + UUID.randomUUID(),
                "false"));
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer();
    }

    private void waitForPartitionAssignment(Consumer<String, String> consumer) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (consumer.assignment().isEmpty() && System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(100));
        }

        assertThat(consumer.assignment()).isNotEmpty();
    }

    private String headerValue(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        assertThat(header).isNotNull();
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private DeliveryCreatedEvent createDeliveryCreatedEvent() {
        DeliveryCreatedEvent.DeliveryCreatedData data = new DeliveryCreatedEvent.DeliveryCreatedData(
                ORDER_ID,
                DELIVERY_ID,
                LocalDateTime.of(2026, 8, 14, 18, 0),
                "도착 전 연락",
                "서울특별시 중구 세종대로 1",
                List.of(new DeliveryCreatedEvent.Product(UUID.randomUUID(), 4)),
                UUID.randomUUID(),
                UUID.randomUUID(),
                11_000L,
                List.of(),
                new DeliveryCreatedEvent.DeliveryManagerWorkingHours(
                        LocalTime.of(9, 0), LocalTime.of(18, 0)),
                List.of());

        return new DeliveryCreatedEvent(
                UUID.randomUUID(), LocalDateTime.of(2026, 8, 12, 10, 0), data);
    }
}
