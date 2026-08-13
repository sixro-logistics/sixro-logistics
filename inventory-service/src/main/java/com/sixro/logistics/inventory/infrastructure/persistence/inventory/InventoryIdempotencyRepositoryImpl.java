package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryIdempotencyRepositoryImpl implements InventoryIdempotencyRepository {

    private final InventoryIdempotencyJpaRepository inventoryIdempotencyJpaRepository;

    @Override
    public boolean existsByIdempotencyKey(UUID idempotencyKey) {
        return inventoryIdempotencyJpaRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public void save(InventoryIdempotency inventoryIdempotency) {
        inventoryIdempotencyJpaRepository.save(inventoryIdempotency);
    }

}
