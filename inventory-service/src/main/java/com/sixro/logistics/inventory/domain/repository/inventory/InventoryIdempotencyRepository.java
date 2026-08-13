package com.sixro.logistics.inventory.domain.repository.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;

import java.util.UUID;

public interface InventoryIdempotencyRepository {

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    void save(InventoryIdempotency inventoryIdempotency);
}
