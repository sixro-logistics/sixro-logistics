package com.sixro.logistics.order.infrastructure.persistence;

import com.sixro.logistics.order.domain.entity.Order;
import com.sixro.logistics.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderQueryRepository orderQueryRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }
}
