package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.order.application.command.OrderCreateServiceCommand;
import com.sixro.logistics.order.application.mapper.order.OrderEventMapper;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.application.result.OrderResultItem;
import com.sixro.logistics.order.application.service.outbox.OutboxService;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import com.sixro.logistics.order.domain.repository.order.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    @Transactional
    public OrderCreateResult createOrder(OrderCreateServiceCommand command) {

        Order order = Order.create(
                command.hubId(), command.ordererId(), command.receiverCompanyId(),
                command.deliveryAddress(), command.deliveryDeadline(), command.requests()
        );

        Order createdOrder = orderRepository.save(order);

        List<OrderItem> orderItems = command.orderItems().stream()
                .map(item
                        -> OrderItem.create(
                                createdOrder.getId(),
                                item.productId(),
                                item.productName(),
                                item.productPrice(),
                                item.quantity(),
                                item.companyId()
                        )
                ).toList();

        List<OrderItem> createdItems = orderRepository.saveAllOrderItems(orderItems);

        OrderCreateResult result =  new OrderCreateResult(
                createdOrder.getId(),
                createdOrder.getHubId(),
                createdOrder.getReceiverCompanyId(),
                createdOrder.getDeliveryAddress(),
                createdOrder.getDeliveryDeadline(),
                createdOrder.getRequests(),
                createdOrder.getOrderStatus(),
                createdItems.stream().map(item -> new OrderResultItem(
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductPrice(),
                        item.getQuantity(),
                        item.getCompanyId()
                )).toList()
        );

        OrderCreatedEvent event =
                OrderEventMapper.toEvent(createdOrder, createdItems);

        outboxService.save(event);

        return result;
    }

}
