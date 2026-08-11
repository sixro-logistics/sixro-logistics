package com.sixro.logistics.delivery.infrastructure.kafka.consumer;

import com.sixro.logistics.delivery.application.command.CreateDeliveryCommand;
import com.sixro.logistics.delivery.application.command.CreateDeliveryItemCommand;
import com.sixro.logistics.delivery.application.service.DeliveryCreationService;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderConfirmedData;
import com.sixro.logistics.delivery.infrastructure.kafka.event.OrderConfirmedEvent;
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
public class OrderConfirmedEventConsumer {

    private final DeliveryCreationService deliveryCreationService;

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED)
    public void consume(OrderConfirmedEvent event,
                        @Header(name = "trace-id", required = false) String traceId) {

        // 이벤트 추적 ID 확인
        String resolvedTraceId = resolveTraceId(traceId);

        // 주문 확정 이벤트 수신
        OrderConfirmedData data = event.data();
        log.info("OrderConfirmedEvent 수신: eventId={}, orderId={}, traceId={}",
                event.eventId(), data.orderId(), resolvedTraceId);

        // 배송 생성 요청 변환
        CreateDeliveryCommand command = toCommand(event, resolvedTraceId);

        // 배송 생성 처리
        boolean created = deliveryCreationService.createDelivery(command);
        if (!created) {
            log.info("중복 OrderConfirmedEvent 처리 생략: eventId={}, orderId={}, traceId={}",
                    event.eventId(), data.orderId(), resolvedTraceId);
            return;
        }

        log.info("OrderConfirmedEvent 배송 생성 완료: eventId={}, orderId={}, traceId={}",
                event.eventId(), data.orderId(), resolvedTraceId);
    }

    private CreateDeliveryCommand toCommand(OrderConfirmedEvent event, String traceId) {
        OrderConfirmedData data = event.data();
        List<CreateDeliveryItemCommand> orderItems = data.items() == null
                ? null
                : data.items().stream()
                .map(item -> new CreateDeliveryItemCommand(item.companyId(), item.productId(), item.quantity()))
                .toList();

        return new CreateDeliveryCommand(
                traceId, data.orderId(), data.hubId(), data.receiverCompanyId(), data.receiverId(),
                data.deliveryAddress(), data.deliveryDeadline(), data.requests(), orderItems);
    }

    private String resolveTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}
