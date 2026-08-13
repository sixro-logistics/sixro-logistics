package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.order.domain.entity.order.OrderIdempotency;
import com.sixro.logistics.order.domain.repository.order.OrderIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderIdempotencyService {

    private final OrderIdempotencyRepository orderIdempotencyRepository;

    public Optional<OrderIdempotency> find(UUID idempotencyKey) {
        return orderIdempotencyRepository.findByIdempotencyKey(idempotencyKey);
    }

    public void save(
            UUID idempotencyKey,
            UUID orderId
    ) {
        orderIdempotencyRepository.save(
                OrderIdempotency.create(
                        idempotencyKey,
                        orderId
                )
        );
    }
}