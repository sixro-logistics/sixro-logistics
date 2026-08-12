package com.sixro.logistics.order.infrastructure.persistence.order;

import com.sixro.logistics.order.application.command.OrderSearchCommand;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
import com.sixro.logistics.order.domain.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderQueryRepository orderQueryRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findByIdAndIsDeletedFalse(UUID orderId) {
        return orderJpaRepository.findByIdAndIsDeletedFalse(orderId);
    }

    @Override
    public Optional<Order> findForUpdateByIdAndIsDeletedFalse(UUID orderID) {
        return orderJpaRepository.findByIdAndIsDeletedFalse(orderID);
    }

    @Override
    public Optional<Order> findByIdWithItems(UUID orderId) {
        return orderJpaRepository.findByIdWithItems(orderId);
    }

    @Override
    public Page<Order> findAll(
            UUID userId, UserRole userRole, UUID affiliationId,
            OrderSearchCommand command, Pageable pageable
    ) {
        return orderQueryRepository.findAll(userId, userRole, affiliationId,
                command, pageable);
    }

}
