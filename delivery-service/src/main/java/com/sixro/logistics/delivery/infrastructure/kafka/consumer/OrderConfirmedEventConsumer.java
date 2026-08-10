package com.sixro.logistics.delivery.infrastructure.kafka.consumer;

import com.sixro.logistics.delivery.application.command.CreateDeliveryCommand;
import com.sixro.logistics.delivery.application.command.CreateDeliveryItemCommand;
import com.sixro.logistics.delivery.application.service.DeliveryCreationService;
import com.sixro.logistics.delivery.infrastructure.kafka.KafkaTopics;
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
        log.info("OrderConfirmedEvent 수신: orderId={}", event.orderId());

        // 배송 생성 요청 변환
        CreateDeliveryCommand command = toCommand(event);

        // 배송 생성 처리
        boolean created = deliveryCreationService.createDelivery(command);
        if (!created) {
            log.info("중복 OrderConfirmedEvent 처리 생략: orderId={}", event.orderId());
            return;
        }

        log.info("OrderConfirmedEvent 배송 생성 완료: orderId={}", event.orderId());
    }

    private CreateDeliveryCommand toCommand(OrderConfirmedEvent event) {
        List<CreateDeliveryItemCommand> orderItems = event.orderItems() == null
                ? null
                : event.orderItems().stream()
                .map(item -> new CreateDeliveryItemCommand(item.companyId(), item.productId(), item.quantity()))
                .toList();

        return new CreateDeliveryCommand(
                event.orderId(), event.hubId(), event.receiverCompanyId(), event.receiverId(),
                event.deliveryAddress(), event.deliveryDeadline(), event.requests(), orderItems);
    }
}
