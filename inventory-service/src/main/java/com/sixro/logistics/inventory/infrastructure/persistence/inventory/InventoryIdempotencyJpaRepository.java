package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryIdempotencyJpaRepository
        extends JpaRepository<InventoryIdempotency, UUID> {

    boolean existsByIdempotencyKey(UUID idempotencyKey);

}
