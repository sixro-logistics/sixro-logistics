package com.sixro.logistics.order.infrastructure.persistence.order;

import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
import com.sixro.logistics.order.domain.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderQueryRepository orderQueryRepository;

    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public List<OrderItem> saveAllOrderItems(List<OrderItem> items) {
        return orderItemJpaRepository.saveAll(items);
    }
}
