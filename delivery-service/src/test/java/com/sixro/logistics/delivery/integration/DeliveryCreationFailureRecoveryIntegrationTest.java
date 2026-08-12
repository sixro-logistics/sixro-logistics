package com.sixro.logistics.delivery.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.delivery.application.port.CompanyQueryPort;
import com.sixro.logistics.delivery.application.port.HubRouteQueryPort;
import com.sixro.logistics.delivery.application.port.UserQueryPort;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedData;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedItem;
import com.sixro.logistics.delivery.infrastructure.kafka.outbox.OutboxPublisher;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRepository;
import com.sixro.logistics.delivery.infrastructure.repository.OutboxRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        KafkaTestContainerConfig.class
})
class DeliveryCreationFailureRecoveryIntegrationTest {

    private static final String TRACE_ID = "delivery-failure-integration-trace-id";
    private static final UUID EVENT_ID = UUID.fromString("02000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("14000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID = UUID.fromString("14000000-0000-0000-0000-000000000002");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("24000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_COMPANY_ID = UUID.fromString("34000000-0000-0000-0000-000000000001");
    private static final UUID SUPPLIER_COMPANY_ID = UUID.fromString("34000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("44000000-0000-0000-0000-000000000001");

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private KafkaListenerEndpointRegistry listenerEndpointRegistry;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor scheduledTaskProcessor;

    @MockitoBean
    private UserQueryPort userQueryPort;

    @MockitoBean
    private CompanyQueryPort companyQueryPort;

    @MockitoBean
    private HubRouteQueryPort hubRouteQueryPort;

    @BeforeEach
    void prepareKafkaFlow() {
        // Recoverer가 저장한 PENDING Outbox를 테스트가 직접 발행하도록 예약 실행을 중지합니다.
        scheduledTaskProcessor.getScheduledTasks().forEach(task -> task.cancel(false));
        kafkaAdmin.createOrModifyTopics(
                new NewTopic(KafkaTopics.ORDER_CREATED, 1, (short) 1),
                new NewTopic(KafkaTopics.DELIVERY_CREATION_FAILED, 1, (short) 1));
        listenerEndpointRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container, 1));
    }

    @Test
    @DisplayName("주문 이벤트 처리 재시도가 소진되면 실패 Outbox를 저장하고 Kafka로 발행한다")
    void consumeFailure_afterRetries_savesAndPublishesFailureEvent() throws Exception {
        // given
        when(userQueryPort.findUser(RECEIVER_ID)).thenReturn(Optional.empty());
        OrderCreatedEvent event = createOrderCreatedEvent();

        try (Consumer<String, String> failureConsumer = createKafkaConsumer()) {
            failureConsumer.subscribe(List.of(KafkaTopics.DELIVERY_CREATION_FAILED));
            waitForPartitionAssignment(failureConsumer);

            ProducerRecord<String, Object> orderRecord = new ProducerRecord<>(
                    KafkaTopics.ORDER_CREATED, ORDER_ID.toString(), event);
            orderRecord.headers().add(
                    new RecordHeader("trace-id", TRACE_ID.getBytes(StandardCharsets.UTF_8)));

            // when
            kafkaTemplate.send(orderRecord).get(10, TimeUnit.SECONDS);

            // 최초 처리와 3회 재시도 후 Recoverer가 실패 Outbox를 저장할 때까지 대기
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                List<Outbox> pendingOutboxes = outboxRepository
                        .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
                assertThat(pendingOutboxes)
                        .singleElement()
                        .extracting(Outbox::getEventType)
                        .isEqualTo(OutboxEventType.DELIVERY_CREATION_FAILED);
            });

            Outbox pendingOutbox = outboxRepository
                    .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst();
            assertThat(pendingOutbox.getAggregateId()).isEqualTo(ORDER_ID);
            assertThat(pendingOutbox.getTraceId()).isEqualTo(TRACE_ID);

            // 실패 Outbox를 실제 Kafka Topic으로 발행
            outboxPublisher.publish();
            ConsumerRecord<String, String> failureRecord = KafkaTestUtils.getSingleRecord(
                    failureConsumer, KafkaTopics.DELIVERY_CREATION_FAILED, Duration.ofSeconds(10));

            // then
            assertThat(failureRecord.key()).isEqualTo(ORDER_ID.toString());
            assertThat(headerValue(failureRecord, "event-type"))
                    .isEqualTo("DeliveryCreationFailedEvent");
            assertThat(headerValue(failureRecord, "trace-id")).isEqualTo(TRACE_ID);

            JsonNode failedEvent = objectMapper.readTree(failureRecord.value());
            assertThat(failedEvent.path("data").path("orderCreatedEventId").asText())
                    .isEqualTo(EVENT_ID.toString());
            assertThat(failedEvent.path("data").path("orderId").asText())
                    .isEqualTo(ORDER_ID.toString());
            assertThat(failedEvent.path("data").path("failureCode").asText())
                    .isEqualTo(DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND.name());
            assertThat(failedEvent.path("data").path("failureMessage").asText())
                    .isEqualTo(DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND.getMessage());

            Outbox publishedOutbox = outboxRepository.findById(pendingOutbox.getEventId()).orElseThrow();
            assertThat(publishedOutbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(publishedOutbox.getPublishedAt()).isNotNull();
        }

        // 실패한 주문으로 배송이 생성되지 않고 외부 후속 조회도 진행되지 않았는지 확인
        assertThat(deliveryRepository.findByOrderId(ORDER_ID)).isEmpty();
        verify(userQueryPort, times(4)).findUser(RECEIVER_ID);
        verify(companyQueryPort, never()).findHubInfo(RECEIVER_COMPANY_ID);
        verify(hubRouteQueryPort, never()).findPath(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    private Consumer<String, String> createKafkaConsumer() {
        Map<String, Object> properties = new HashMap<>(KafkaTestUtils.consumerProps(
                kafkaConnectionDetails.getBootstrapServers().getFirst(),
                "delivery-failure-integration-" + UUID.randomUUID(),
                "false"));
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<String, String>(properties).createConsumer();
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

    private OrderCreatedEvent createOrderCreatedEvent() {
        OrderCreatedData data = new OrderCreatedData(
                ORDER_ID,
                RECEIVER_ID,
                ORIGIN_HUB_ID,
                RECEIVER_COMPANY_ID,
                "서울특별시 중구 세종대로 1",
                LocalDateTime.of(2026, 8, 14, 18, 0),
                "도착 전 연락",
                List.of(new OrderCreatedItem(SUPPLIER_COMPANY_ID, PRODUCT_ID, 4)));

        return new OrderCreatedEvent(EVENT_ID, LocalDateTime.of(2026, 8, 12, 10, 0), data);
    }
}
