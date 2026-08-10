package com.sixro.logistics.order.domain.repository.order;

import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;

import java.util.List;

public interface OrderRepository {

    Order save(Order order);

    //Optional<Order> findByIdAndIsDeletedFalse(UUID id);

    List<OrderItem> saveAllOrderItems(List<OrderItem> items);

}
