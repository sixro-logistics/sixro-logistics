package com.sixro.logistics.order.application.mapper.order;

import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import com.sixro.logistics.order.domain.event.order.OrderCreatedItem;

public class OrderEventMapper {

    public static OrderCreatedEvent toEvent(OrderCreateResult result) {

        return new OrderCreatedEvent(
                result.orderId(),
                result.hubId(),
                result.orderItems().stream()
                        .map(item -> new OrderCreatedItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );
    }
}