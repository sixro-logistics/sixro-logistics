package com.sixro.logistics.inventory.application.service.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;
import com.sixro.logistics.inventory.domain.entity.inventory.InventoryOperation;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryIdempotencyService {

    private final InventoryIdempotencyRepository inventoryIdempotencyRepository;

    public boolean isProcessed(UUID idempotencyKey) {
        return inventoryIdempotencyRepository
                .existsByIdempotencyKey(idempotencyKey);
    }

    public void save(
            UUID idempotencyKey,
            InventoryOperation operation
    ) {
        inventoryIdempotencyRepository.save(
                InventoryIdempotency.create(
                        idempotencyKey,
                        operation
                )
        );
    }
}