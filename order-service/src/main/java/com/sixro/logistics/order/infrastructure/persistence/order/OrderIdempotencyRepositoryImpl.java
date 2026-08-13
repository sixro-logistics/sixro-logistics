package com.sixro.logistics.order.infrastructure.persistence.order;

import com.sixro.logistics.order.domain.entity.order.OrderIdempotency;
import com.sixro.logistics.order.domain.repository.order.OrderIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderIdempotencyRepositoryImpl
        implements OrderIdempotencyRepository {

    private final OrderIdempotencyJpaRepository orderIdempotencyJpaRepository;

    @Override
    public Optional<OrderIdempotency> findByIdempotencyKey(UUID idempotencyKey) {
        return orderIdempotencyJpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public void save(OrderIdempotency orderIdempotency) {
        orderIdempotencyJpaRepository.save(orderIdempotency);
    }
}