package com.sixro.logistics.delivery.infrastructure.kafka.consumer;

import com.sixro.logistics.delivery.application.command.CreateDeliveryCommand;
import com.sixro.logistics.delivery.application.command.CreateDeliveryItemCommand;
import com.sixro.logistics.delivery.application.service.DeliveryCreationService;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedData;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderCreatedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final DeliveryCreationService deliveryCreationService;
    private final Tracer tracer;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED)
    public void consume(OrderCreatedEvent event,
                        @Header(name = "trace-id", required = false) String originalTraceId) {

        // 이벤트 Trace ID 확인
        String traceId = resolveTraceId(originalTraceId, event.eventId());

        // 주문 생성 이벤트 수신
        OrderCreatedData data = event.data();
        log.info("OrderCreatedEvent 수신: eventId={}, orderId={}, traceId={}",
                event.eventId(), data.orderId(), traceId);

        // 배송 생성 요청 변환
        CreateDeliveryCommand command = toCommand(event, traceId);

        // 배송 생성 처리
        boolean created = deliveryCreationService.createDelivery(command);
        if (!created) {
            log.info("중복 OrderCreatedEvent 처리 생략: eventId={}, orderId={}, traceId={}",
                    event.eventId(), data.orderId(), traceId);
            return;
        }

        log.info("OrderCreatedEvent 배송 생성 완료: eventId={}, orderId={}, traceId={}",
                event.eventId(), data.orderId(), traceId);
    }

    private CreateDeliveryCommand toCommand(OrderCreatedEvent event, String traceId) {
        OrderCreatedData data = event.data();
        List<CreateDeliveryItemCommand> orderItems = data.items() == null
                ? null
                : data.items().stream()
                .map(item -> new CreateDeliveryItemCommand(item.companyId(), item.productId(), item.quantity()))
                .toList();

        return new CreateDeliveryCommand(
                traceId, data.orderId(), data.hubId(), data.receiverCompanyId(), data.receiverId(),
                data.deliveryAddress(), data.deliveryDeadline(), data.requests(), orderItems);
    }

    private String resolveTraceId(String originalTraceId, UUID orderCreatedEventId) {
        if (originalTraceId != null && !originalTraceId.isBlank()) {
            return originalTraceId;
        }

        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return orderCreatedEventId.toString();
        }
        return currentSpan.context().traceId();
    }
}
