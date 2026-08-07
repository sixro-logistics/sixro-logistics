package com.sixro.logistics.order.application.mapper.order;

import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import com.sixro.logistics.order.domain.event.order.OrderCreatedItem;

import java.util.List;

public class OrderEventMapper {

    public static OrderCreatedEvent toEvent(
            Order order,
            List<OrderItem> orderItems
    ) {

        return new OrderCreatedEvent(
                order.getId(),
                order.getHubId(),
                orderItems.stream()
                        .map(item -> new OrderCreatedItem(
                                item.getProductId(),
                                item.getQuantity()
                        ))
                        .toList()
        );
    }
}