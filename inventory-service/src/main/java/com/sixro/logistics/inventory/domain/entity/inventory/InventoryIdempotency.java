package com.sixro.logistics.inventory.domain.entity.inventory;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "p_inventory_idempotency",
        schema = "inventory_schema",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false
    )
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private InventoryOperation operation;

    private InventoryIdempotency(
            UUID idempotencyKey,
            InventoryOperation operation
    ) {
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
    }

    public static InventoryIdempotency create(
            UUID idempotencyKey,
            InventoryOperation operation
    ) {
        return new InventoryIdempotency(
                idempotencyKey,
                operation
        );
    }
}