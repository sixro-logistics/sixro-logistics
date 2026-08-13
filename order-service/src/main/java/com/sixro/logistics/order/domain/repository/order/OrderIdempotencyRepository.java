package com.sixro.logistics.order.domain.repository.order;

import com.sixro.logistics.order.domain.entity.order.OrderIdempotency;

import java.util.Optional;
import java.util.UUID;

public interface OrderIdempotencyRepository {

    Optional<OrderIdempotency> findByIdempotencyKey(
            UUID idempotencyKey
    );

    void save(OrderIdempotency orderIdempotency);

}
