package com.sixro.logistics.delivery.infrastructure.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.application.event.DeliveryCreationFailedEvent;
import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxEventType;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import com.sixro.logistics.delivery.domain.port.OutboxRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.kafka.producer.DeliveryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String TRACE_ID = "outbox-trace-id";
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DELIVERY_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 12, 10, 0);

    @Mock
    private OutboxRepositoryPort outboxRepositoryPort;

    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    private ObjectMapper objectMapper;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        outboxPublisher = new OutboxPublisher(outboxRepositoryPort, deliveryEventProducer, objectMapper);
    }

    @Test
    @DisplayName("PENDING 배송 생성 이벤트 발행에 성공하면 PUBLISHED로 변경한다")
    void publish_deliveryCreatedSuccess_marksPublished() throws Exception {
        // given
        DeliveryCreatedEvent event = createDeliveryCreatedEvent();
        Outbox outbox = createOutbox(
                DELIVERY_ID,
                OutboxEventType.DELIVERY_CREATED,
                objectMapper.writeValueAsString(event)
        );
        when(outboxRepositoryPort.findPendingOutboxes()).thenReturn(List.of(outbox));

        // when
        outboxPublisher.publish();

        // then
        verify(deliveryEventProducer).sendDeliveryCreatedEvent(event, TRACE_ID);
        verify(deliveryEventProducer, never())
                .sendDeliveryCreationFailedEvent(any(DeliveryCreationFailedEvent.class), any());
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Kafka 발행에 실패하면 Outbox를 PENDING 상태로 유지한다")
    void publish_kafkaFailure_keepsPending() throws Exception {
        // given
        DeliveryCreatedEvent event = createDeliveryCreatedEvent();
        Outbox outbox = createOutbox(
                DELIVERY_ID,
                OutboxEventType.DELIVERY_CREATED,
                objectMapper.writeValueAsString(event)
        );
        when(outboxRepositoryPort.findPendingOutboxes()).thenReturn(List.of(outbox));
        doThrow(new ExecutionException(new RuntimeException("kafka failure")))
                .when(deliveryEventProducer).sendDeliveryCreatedEvent(any(DeliveryCreatedEvent.class), any());

        // when
        outboxPublisher.publish();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("배송 생성 실패 Outbox는 실패 이벤트 Producer로 발행한다")
    void publish_deliveryCreationFailed_usesFailureProducer() throws Exception {
        // given
        DeliveryCreationFailedEvent event = createDeliveryCreationFailedEvent();
        Outbox outbox = createOutbox(
                ORDER_ID,
                OutboxEventType.DELIVERY_CREATION_FAILED,
                objectMapper.writeValueAsString(event)
        );
        when(outboxRepositoryPort.findPendingOutboxes()).thenReturn(List.of(outbox));

        // when
        outboxPublisher.publish();

        // then
        verify(deliveryEventProducer).sendDeliveryCreationFailedEvent(event, TRACE_ID);
        verify(deliveryEventProducer, never())
                .sendDeliveryCreatedEvent(any(DeliveryCreatedEvent.class), any());
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
    }

    private Outbox createOutbox(UUID aggregateId, OutboxEventType eventType, String payload) {
        return Outbox.create(UUID.randomUUID(), aggregateId, eventType, TRACE_ID, payload, OCCURRED_AT);
    }

    private DeliveryCreatedEvent createDeliveryCreatedEvent() {
        DeliveryCreatedEvent.DeliveryCreatedData data = new DeliveryCreatedEvent.DeliveryCreatedData(
                ORDER_ID,
                DELIVERY_ID,
                LocalDateTime.of(2026, 8, 14, 18, 0),
                null,
                "서울특별시 중구 세종대로 1",
                List.of(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                0L,
                List.of(),
                new DeliveryCreatedEvent.DeliveryManagerWorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                List.of()
        );
        return new DeliveryCreatedEvent(UUID.randomUUID(), OCCURRED_AT, data);
    }

    private DeliveryCreationFailedEvent createDeliveryCreationFailedEvent() {
        DeliveryCreationFailedEvent.DeliveryCreationFailedData data =
                new DeliveryCreationFailedEvent.DeliveryCreationFailedData(
                        UUID.randomUUID(),
                        ORDER_ID,
                        DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED,
                        DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED.getMessage()
                );
        return new DeliveryCreationFailedEvent(UUID.randomUUID(), OCCURRED_AT, data);
    }
}
