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
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedEventConsumer {

    private final DeliveryCreationService deliveryCreationService;

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED)
    public void consume(OrderConfirmedEvent event) {
        // 주문 확정 이벤트 수신
        OrderConfirmedData data = event.data();
        log.info("OrderConfirmedEvent 수신: eventId={}, orderId={}", event.eventId(), data.orderId());

        // 배송 생성 요청 변환
        CreateDeliveryCommand command = toCommand(event);

        // 배송 생성 처리
        boolean created = deliveryCreationService.createDelivery(command);
        if (!created) {
            log.info("중복 OrderConfirmedEvent 처리 생략: eventId={}, orderId={}",
                    event.eventId(), data.orderId());
            return;
        }

        log.info("OrderConfirmedEvent 배송 생성 완료: eventId={}, orderId={}",
                event.eventId(), data.orderId());
    }

    private CreateDeliveryCommand toCommand(OrderConfirmedEvent event) {
        OrderConfirmedData data = event.data();
        List<CreateDeliveryItemCommand> orderItems = data.items() == null
                ? null
                : data.items().stream()
                .map(item -> new CreateDeliveryItemCommand(item.companyId(), item.productId(), item.quantity()))
                .toList();

        return new CreateDeliveryCommand(
                data.orderId(), data.hubId(), data.receiverCompanyId(), data.receiverId(),
                data.deliveryAddress(), data.deliveryDeadline(), data.requests(), orderItems);
    }
}
