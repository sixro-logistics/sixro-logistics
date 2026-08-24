package com.sixro.logistics.order.domain.entity.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "p_order_idempotency",
        schema = "order_schema",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    // 동일한 멱등키로 다른 요청이 들어오는 것을 구분하기 위해 요청 내용을 해시로 저장
    @Column(name = "request_hash", nullable = false, updatable = false)
    private String requestHash;

    // 주문 생성 성공 후에만 멱등키를 기록하지 않고, 요청 처리 단계에 따라 상태를 관리하기 위해 추가
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderIdempotencyStatus status;

    @Column(name = "order_id")
    private UUID orderId;

    private OrderIdempotency(
            UUID idempotencyKey,
            String requestHash
    ) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = OrderIdempotencyStatus.PROCESSING;
    }

    public static OrderIdempotency create(
            UUID idempotencyKey,
            String requestHash
    ) {
        return new OrderIdempotency(
                idempotencyKey,
                requestHash
        );
    }

    public void succeed(UUID orderId) {
        this.orderId = orderId;
        this.status = OrderIdempotencyStatus.SUCCEEDED;
    }

    public void compensate() {
        this.status = OrderIdempotencyStatus.COMPENSATED;
    }

}