package com.sixro.logistics.inventory.infrastructure.persistence;

import com.sixro.logistics.inventory.domain.entity.Inventory;
import com.sixro.logistics.inventory.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// 서비스에서 주입 받는 빈, 스프링이 InventoryRepository을 보고 InventoryRepositoryImpl을 찾아서 자동 주입해줌
@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryQueryRepository inventoryQueryRepository;

    @Override
    public Inventory save(Inventory inventory) {
        return inventoryJpaRepository.save(inventory);
    }

    /*@Override
    public Optional<Inventory> findByIdAndIsDeletedFalse(UUID id) {
        return inventoryJpaRepository.findByIdAndIsDeletedFalse(id);
    }*/
}
