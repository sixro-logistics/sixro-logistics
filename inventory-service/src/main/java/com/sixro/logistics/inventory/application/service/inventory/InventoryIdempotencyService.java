package com.sixro.logistics.inventory.application.service.inventory;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.domain.entity.inventory.InventoryIdempotency;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryIdempotencyRepository;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryIdempotencyService {

    private final InventoryIdempotencyRepository inventoryIdempotencyRepository;

    public Optional<InventoryIdempotency> find(UUID idempotencyKey) {
        return inventoryIdempotencyRepository
                .findByIdempotencyKey(idempotencyKey);
    }

    public void reserve(UUID idempotencyKey) {
        inventoryIdempotencyRepository.save(
                InventoryIdempotency.create(idempotencyKey)
        );
    }

    public void release(UUID idempotencyKey) {
        InventoryIdempotency idempotency =
                inventoryIdempotencyRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() ->
                                new BaseException(InventoryErrorCode.IDEMPOTENCY_NOT_FOUND)
                        );

        idempotency.release();
    }
}