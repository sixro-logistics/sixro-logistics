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

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    private OrderIdempotency(
            UUID idempotencyKey,
            UUID orderId
    ) {
        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
    }

    public static OrderIdempotency create(
            UUID idempotencyKey,
            UUID orderId
    ) {
        return new OrderIdempotency(
                idempotencyKey,
                orderId
        );
    }

}