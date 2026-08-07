package com.sixro.logistics.order.infrastructure.persistence;

import com.sixro.logistics.order.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    //Optional<Order> findByIdAndIsDeletedFalse(UUID id);

}
