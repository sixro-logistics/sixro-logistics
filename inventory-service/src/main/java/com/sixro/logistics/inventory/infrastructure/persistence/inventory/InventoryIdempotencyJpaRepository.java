package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryIdempotencyJpaRepository
        extends JpaRepository<InventoryIdempotency, UUID> {

    Optional<InventoryIdempotency> findByIdempotencyKey(UUID idempotencyKey);

}
