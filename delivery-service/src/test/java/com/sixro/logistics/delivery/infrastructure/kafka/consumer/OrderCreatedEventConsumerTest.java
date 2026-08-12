package com.sixro.logistics.delivery.infrastructure.kafka.consumer;

import com.sixro.logistics.delivery.application.command.CreateDeliveryCommand;
import com.sixro.logistics.delivery.application.service.DeliveryCreationService;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedData;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedEvent;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedItem;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID RECEIVER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_COMPANY_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID SUPPLIER_COMPANY_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 8, 14, 18, 0);
    private static final String CURRENT_SPAN_TRACE_ID = "current-span-trace-id";

    @Mock
    private DeliveryCreationService deliveryCreationService;

    @Mock
    private Tracer tracer;

    @Mock
    private Span currentSpan;

    @Mock
    private TraceContext traceContext;

    @InjectMocks
    private OrderCreatedEventConsumer consumer;

    @Test
    @DisplayName("OrderCreatedEvent를 배송 생성 Command로 정확히 변환한다")
    void consume_convertsEventToCommand() {
        // given
        OrderCreatedEvent event = createEvent();
        when(deliveryCreationService.createDelivery(any(CreateDeliveryCommand.class))).thenReturn(true);

        // when
        consumer.consume(event, "original-trace-id");

        // then
        ArgumentCaptor<CreateDeliveryCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateDeliveryCommand.class);
        verify(deliveryCreationService).createDelivery(commandCaptor.capture());

        CreateDeliveryCommand command = commandCaptor.getValue();
        assertThat(command.getTraceId()).isEqualTo("original-trace-id");
        assertThat(command.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(command.getReceiverId()).isEqualTo(RECEIVER_ID);
        assertThat(command.getOriginHubId()).isEqualTo(ORIGIN_HUB_ID);
        assertThat(command.getReceiverCompanyId()).isEqualTo(RECEIVER_COMPANY_ID);
        assertThat(command.getDeliveryAddress()).isEqualTo("서울특별시 중구 세종대로 1");
        assertThat(command.getDeliveryDeadline()).isEqualTo(DEADLINE);
        assertThat(command.getRequests()).isEqualTo("도착 전 연락");
        assertThat(command.getOrderItems()).singleElement().satisfies(item -> {
            assertThat(item.getCompanyId()).isEqualTo(SUPPLIER_COMPANY_ID);
            assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(item.getQuantity()).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("trace-id와 현재 Span이 없으면 이벤트 ID를 Trace ID로 사용한다")
    void consume_withoutTraceIdOrSpan_usesEventId() {
        // given
        OrderCreatedEvent event = createEvent();
        when(tracer.currentSpan()).thenReturn(null);
        when(deliveryCreationService.createDelivery(any(CreateDeliveryCommand.class))).thenReturn(true);

        // when
        consumer.consume(event, null);

        // then
        ArgumentCaptor<CreateDeliveryCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateDeliveryCommand.class);
        verify(deliveryCreationService).createDelivery(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getTraceId()).isEqualTo(EVENT_ID.toString());
    }

    @Test
    @DisplayName("trace-id가 없고 현재 Span이 있으면 Span의 Trace ID를 사용한다")
    void consume_withoutTraceId_usesCurrentSpanTraceId() {
        // given
        OrderCreatedEvent event = createEvent();
        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(CURRENT_SPAN_TRACE_ID);
        when(deliveryCreationService.createDelivery(any(CreateDeliveryCommand.class))).thenReturn(true);

        // when
        consumer.consume(event, null);

        // then
        ArgumentCaptor<CreateDeliveryCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateDeliveryCommand.class);
        verify(deliveryCreationService).createDelivery(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getTraceId()).isEqualTo(CURRENT_SPAN_TRACE_ID);
    }

    @Test
    @DisplayName("중복 이벤트 처리 결과가 false여도 예외 없이 종료한다")
    void consume_duplicateEvent_completesWithoutException() {
        // given
        OrderCreatedEvent event = createEvent();
        when(deliveryCreationService.createDelivery(any(CreateDeliveryCommand.class))).thenReturn(false);

        // when
        // then
        assertThatCode(() -> consumer.consume(event, "duplicate-trace-id"))
                .doesNotThrowAnyException();
        verify(deliveryCreationService).createDelivery(any(CreateDeliveryCommand.class));
    }

    private OrderCreatedEvent createEvent() {
        OrderCreatedData data = new OrderCreatedData(
                ORDER_ID,
                RECEIVER_ID,
                ORIGIN_HUB_ID,
                RECEIVER_COMPANY_ID,
                "서울특별시 중구 세종대로 1",
                DEADLINE,
                "도착 전 연락",
                List.of(new OrderCreatedItem(SUPPLIER_COMPANY_ID, PRODUCT_ID, 4))
        );
        return new OrderCreatedEvent(EVENT_ID, LocalDateTime.of(2026, 8, 12, 10, 0), data);
    }
}
