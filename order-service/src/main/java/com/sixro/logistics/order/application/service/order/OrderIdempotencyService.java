package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.order.domain.entity.order.OrderIdempotency;
import com.sixro.logistics.order.domain.repository.order.OrderIdempotencyRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

    // PROCESSING 저장을 별도 트랜잭션으로 먼저 커밋하여
    // 주문 생성에 실패해도 멱등성 처리 상태가 남아있도록 구현
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(
            UUID idempotencyKey,
            String requestHash
    ) {
        orderIdempotencyRepository.save(
                OrderIdempotency.create(
                        idempotencyKey,
                        requestHash
                )
        );
    }

    public void succeed(
            OrderIdempotency idempotency,
            UUID orderId
    ) {
        idempotency.succeed(orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(UUID idempotencyKey) {

        OrderIdempotency idempotency = orderIdempotencyRepository
                .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "멱등성 정보를 찾을 수 없습니다."
                        ));

        idempotency.compensate();
    }

}