package com.sixro.logistics.delivery.infrastructure.kafka.consumer;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.application.event.DeliveryCreationFailedEvent;
import com.sixro.logistics.delivery.application.service.OutboxService;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedData;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryCreationFailureRecovererTest {

    private static final UUID ORDER_EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final String CURRENT_SPAN_TRACE_ID = "current-span-trace-id";

    @Mock
    private OutboxService outboxService;

    @Mock
    private Tracer tracer;

    @Mock
    private Span currentSpan;

    @Mock
    private TraceContext traceContext;

    @InjectMocks
    private DeliveryCreationFailureRecoverer recoverer;

    @Test
    @DisplayName("재시도가 끝나면 배송 생성 실패 이벤트를 Outbox에 저장한다")
    void accept_afterRetries_savesFailureEventToOutbox() {
        // given
        ConsumerRecord<String, OrderCreatedEvent> record = createRecord();
        record.headers().add("trace-id", "original-trace-id".getBytes(StandardCharsets.UTF_8));

        // when
        recoverer.accept(record, new RuntimeException("temporary failure"));

        // then
        ArgumentCaptor<DeliveryCreationFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreationFailedEvent.class);
        verify(outboxService).save(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq("original-trace-id"));

        DeliveryCreationFailedEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.data().orderCreatedEventId()).isEqualTo(ORDER_EVENT_ID);
        assertThat(event.data().orderId()).isEqualTo(ORDER_ID);
        assertThat(event.data().failureCode())
                .isEqualTo(DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED);
    }

    @Test
    @DisplayName("trace-id 헤더가 없고 현재 Span이 있으면 Span Trace ID로 실패 Outbox를 저장한다")
    void accept_withoutTraceIdHeader_usesCurrentSpanTraceId() {
        // given
        ConsumerRecord<String, OrderCreatedEvent> record = createRecord();
        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(CURRENT_SPAN_TRACE_ID);

        // when
        recoverer.accept(record, new RuntimeException("temporary failure"));

        // then
        ArgumentCaptor<DeliveryCreationFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreationFailedEvent.class);
        verify(outboxService).save(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq(CURRENT_SPAN_TRACE_ID));
        assertThat(eventCaptor.getValue().data().orderCreatedEventId()).isEqualTo(ORDER_EVENT_ID);
        assertThat(eventCaptor.getValue().data().orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("중첩된 BaseException의 상세 배송 생성 실패 코드를 유지한다")
    void accept_baseException_keepsDetailedFailureCode() {
        // given
        ConsumerRecord<String, OrderCreatedEvent> record = createRecord();
        BaseException baseException = new BaseException(
                DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND
        );
        RuntimeException listenerException = new RuntimeException("listener failure", baseException);
        when(tracer.currentSpan()).thenReturn(null);

        // when
        recoverer.accept(record, listenerException);

        // then
        ArgumentCaptor<DeliveryCreationFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreationFailedEvent.class);
        verify(outboxService).save(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq(ORDER_EVENT_ID.toString()));
        assertThat(eventCaptor.getValue().data().failureCode())
                .isEqualTo(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
        assertThat(eventCaptor.getValue().data().failureMessage())
                .isEqualTo(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("알 수 없는 예외는 기본 배송 생성 실패 코드로 변환한다")
    void accept_unknownException_usesDefaultFailureCode() {
        // given
        ConsumerRecord<String, OrderCreatedEvent> record = createRecord();
        when(tracer.currentSpan()).thenReturn(null);

        // when
        recoverer.accept(record, new IllegalStateException("unknown failure"));

        // then
        ArgumentCaptor<DeliveryCreationFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreationFailedEvent.class);
        verify(outboxService).save(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq(ORDER_EVENT_ID.toString()));
        assertThat(eventCaptor.getValue().data().failureCode())
                .isEqualTo(DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED);
        assertThat(eventCaptor.getValue().data().failureMessage())
                .isEqualTo(DeliveryCreationKafkaErrorCode.DELIVERY_CREATION_FAILED.getMessage());
    }

    private ConsumerRecord<String, OrderCreatedEvent> createRecord() {
        OrderCreatedData data = new OrderCreatedData(
                ORDER_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 중구 세종대로 1",
                LocalDateTime.of(2026, 8, 14, 18, 0),
                null,
                List.of()
        );
        OrderCreatedEvent event = new OrderCreatedEvent(
                ORDER_EVENT_ID,
                LocalDateTime.of(2026, 8, 12, 10, 0),
                data
        );
        return new ConsumerRecord<>(KafkaTopics.ORDER_CREATED, 0, 0L, ORDER_ID.toString(), event);
    }
}
