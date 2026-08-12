package com.sixro.logistics.order.domain.repository.order;

import com.sixro.logistics.order.application.command.OrderSearchCommand;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.entity.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByIdAndIsDeletedFalse(UUID orderId);

    Optional<Order> findForUpdateByIdAndIsDeletedFalse(UUID orderID);

    Optional<Order> findByIdWithItems(UUID orderId);

    Page<Order> findAll(
            UUID userId, UserRole userRole, UUID affiliationId,
            OrderSearchCommand command, Pageable pageable
    );

}
