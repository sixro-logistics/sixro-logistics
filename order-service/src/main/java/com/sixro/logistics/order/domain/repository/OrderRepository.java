package com.sixro.logistics.order.domain.repository;

import com.sixro.logistics.order.domain.entity.Order;
import com.sixro.logistics.order.domain.entity.OrderItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    //Optional<Order> findByIdAndIsDeletedFalse(UUID id);

    List<OrderItem> saveAllOrderItems(List<OrderItem> items);

}
