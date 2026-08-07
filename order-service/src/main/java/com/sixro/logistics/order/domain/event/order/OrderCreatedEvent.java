package com.sixro.logistics.order.domain.event.order;

import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;

import java.util.List;
import java.util.UUID;

// 도메인 이벤트
public record OrderCreatedEvent(
        UUID orderId,
        UUID hubId,
        List<OrderCreatedItem> items

) {

    public static OrderCreatedEvent toEvent(
            Order order,
            List<OrderItem> items
    ){
        return new OrderCreatedEvent(
                order.getId(),
                order.getHubId(),
                items.stream()
                        .map(item -> new OrderCreatedItem(
                                item.getProductId(),
                                item.getQuantity()
                        ))
                        .toList()
        );
    }

}