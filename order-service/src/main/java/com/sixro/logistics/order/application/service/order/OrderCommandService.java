package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.command.*;
import com.sixro.logistics.order.application.event.EventEnvelope;
import com.sixro.logistics.order.application.result.*;
import com.sixro.logistics.order.application.service.event.ProcessedEventService;
import com.sixro.logistics.order.application.service.outbox.OutboxService;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
import com.sixro.logistics.order.domain.event.order.*;
import com.sixro.logistics.order.domain.repository.order.OrderRepository;
import com.sixro.logistics.order.exception.OrderErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final ProcessedEventService processedEventService;

    private UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public OrderCreateResult createOrder(OrderCreateServiceCommand command) {

        Order order = Order.create(
                command.hubId(),
                command.receiverId(),
                command.receiverCompanyId(),
                command.deliveryAddress(),
                command.deliveryDeadline(),
                command.requests()
        );

        List<OrderItem> orderItems = command.orderItems().stream()
                .map(item -> OrderItem.create(
                        item.productId(),
                        item.productName(),
                        item.productPrice(),
                        item.quantity(),
                        item.companyId()
                ))
                .toList();

        orderItems.forEach(item -> order.addOrderItem(item));

        Order createdOrder = orderRepository.save(order);

        List<OrderResultItem> createdItems = createdOrder.getItems()
                .stream()
                .map(item -> new OrderResultItem(
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductPrice(),
                        item.getQuantity(),
                        item.getCompanyId()
                ))
                .toList();

        OrderCreateResult result =  new OrderCreateResult(
                createdOrder.getId(),
                createdOrder.getReceiverId(),
                createdOrder.getHubId(),
                createdOrder.getReceiverCompanyId(),
                createdOrder.getDeliveryAddress(),
                createdOrder.getDeliveryDeadline(),
                createdOrder.getRequests(),
                createdOrder.getOrderStatus(),
                createdItems
        );

        OrderCreatedEvent data = new OrderCreatedEvent(
                createdOrder.getId(),
                createdOrder.getReceiverId(),
                createdOrder.getHubId(),
                createdOrder.getReceiverCompanyId(),
                createdOrder.getDeliveryAddress(),
                createdOrder.getDeliveryDeadline(),
                createdOrder.getRequests(),
                createdOrder.getItems()
                        .stream()
                        .map(item -> new OrderCreatedItem(
                                item.getCompanyId(),
                                item.getProductId(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        EventEnvelope<OrderCreatedEvent> event = EventEnvelope.of(data);

        outboxService.saveOrderCreated(event);

        return result;
    }

    public OrderUpdateResult updateOrder(UUID orderId, OrderUpdateCommand command) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() ->
                        new BaseException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.update(
                command.deliveryDeadline(),
                command.requests()
        );

        return new OrderUpdateResult(
                order.getId(),
                order.getDeliveryDeadline(),
                order.getRequests()
        );
    }

    public OrderCancelResult cancelOrder(UUID orderId) {
        Order order = orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() ->
                        new BaseException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.cancel();

        OrderCanceledEvent data = new OrderCanceledEvent(
                order.getId(),
                order.getHubId(),
                order.getItems()
                        .stream()
                        .map(item -> new OrderCanceledItem(
                                item.getProductId(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        EventEnvelope<OrderCanceledEvent> event =
                EventEnvelope.of(data);

        outboxService.saveOrderCanceled(event);

        return new OrderCancelResult(
                order.getId(),
                order.getOrderStatus()
        );
    }

    public OrderDeleteResult deleteOrder(UUID orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() ->
                        new BaseException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.softDelete(SYSTEM_ID);

        return new OrderDeleteResult(
                order.getId(),
                order.getOrderStatus()
        );
    }

    public void deliveryCreated(DeliveryCreatedCommand command) {

        if(processedEventService.isProcessed(command.eventId())){
            return;
        }

        Order order = orderRepository.findForUpdateByIdAndIsDeletedFalse(command.orderId())
                .orElseThrow(() ->
                        new BaseException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.deliveryCreated();

        processedEventService.save(command.eventId());

    }

    public void deliveryCreationFailed(DeliveryCreationFailedCommand command) {

        if(processedEventService.isProcessed(command.eventId())){
            return;
        }

        Order order = orderRepository.findForUpdateByIdAndIsDeletedFalse(command.orderId())
                .orElseThrow(() ->
                        new BaseException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.fail();
        order.softDelete(SYSTEM_ID);

        processedEventService.save(command.eventId());

        // ORDER_FAILED 이벤트 발행

        OrderFailedEvent data = new OrderFailedEvent(
                order.getId(),
                order.getHubId(),
                order.getItems()
                        .stream()
                        .map(item -> new OrderFailedItem(
                                item.getProductId(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        EventEnvelope<OrderFailedEvent> event =
                EventEnvelope.of(data);

        outboxService.saveOrderFailed(event);

    }
}
