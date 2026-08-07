package com.sixro.logistics.order.infrastructure.persistence;

import com.sixro.logistics.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, UUID> {
}
