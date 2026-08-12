package com.sixro.logistics.delivery.infrastructure.kafka.producer;

import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.application.event.DeliveryCreationFailedEvent;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryEventProducerTest {

    private static final String TRACE_ID = "delivery-event-trace-id";
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DELIVERY_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private DeliveryEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new DeliveryEventProducer(kafkaTemplate);
    }

    @Test
    @DisplayName("DeliveryCreatedEvent를 배송 ID Key와 필수 Header로 발행한다")
    void sendDeliveryCreatedEvent_buildsExpectedRecord() throws Exception {
        // given
        DeliveryCreatedEvent event = createDeliveryCreatedEvent();
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, Object>>any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // when
        producer.sendDeliveryCreatedEvent(event, TRACE_ID);

        // then
        ProducerRecord<String, Object> record = captureRecord();
        assertThat(record.topic()).isEqualTo(KafkaTopics.DELIVERY_CREATED);
        assertThat(record.key()).isEqualTo(DELIVERY_ID.toString());
        assertThat(record.value()).isSameAs(event);
        assertThat(headerValue(record, "event-type")).isEqualTo("DeliveryCreatedEvent");
        assertThat(headerValue(record, "trace-id")).isEqualTo(TRACE_ID);
    }

    @Test
    @DisplayName("DeliveryCreationFailedEvent를 주문 ID Key와 필수 Header로 발행한다")
    void sendDeliveryCreationFailedEvent_buildsExpectedRecord() throws Exception {
        // given
        DeliveryCreationFailedEvent event = createDeliveryCreationFailedEvent();
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, Object>>any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // when
        producer.sendDeliveryCreationFailedEvent(event, TRACE_ID);

        // then
        ProducerRecord<String, Object> record = captureRecord();
        assertThat(record.topic()).isEqualTo(KafkaTopics.DELIVERY_CREATION_FAILED);
        assertThat(record.key()).isEqualTo(ORDER_ID.toString());
        assertThat(record.value()).isSameAs(event);
        assertThat(headerValue(record, "event-type")).isEqualTo("DeliveryCreationFailedEvent");
        assertThat(headerValue(record, "trace-id")).isEqualTo(TRACE_ID);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ProducerRecord<String, Object> captureRecord() {
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        return recordCaptor.getValue();
    }

    private String headerValue(ProducerRecord<String, Object> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        assertThat(header).isNotNull();
        return new String(header.value(), StandardCharsets.UTF_8);
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
        return new DeliveryCreatedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 8, 12, 10, 0), data);
    }

    private DeliveryCreationFailedEvent createDeliveryCreationFailedEvent() {
        DeliveryCreationFailedEvent.DeliveryCreationFailedData data =
                new DeliveryCreationFailedEvent.DeliveryCreationFailedData(
                        UUID.randomUUID(),
                        ORDER_ID,
                        DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED,
                        DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED.getMessage()
                );
        return new DeliveryCreationFailedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 8, 12, 10, 0), data);
    }
}
