package com.sixro.logistics.inventory.domain.repository.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;

import java.util.Optional;
import java.util.UUID;

public interface InventoryIdempotencyRepository {

    Optional<InventoryIdempotency> findByIdempotencyKey(UUID idempotencyKey);

    void save(InventoryIdempotency inventoryIdempotency);
}
