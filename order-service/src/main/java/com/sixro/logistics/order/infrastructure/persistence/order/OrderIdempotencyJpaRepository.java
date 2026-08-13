package com.sixro.logistics.order.infrastructure.persistence.order;

import com.sixro.logistics.order.domain.entity.order.OrderIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderIdempotencyJpaRepository extends JpaRepository<OrderIdempotency, UUID> {

    Optional<OrderIdempotency> findByIdempotencyKey(UUID idempotencyKey);

}
