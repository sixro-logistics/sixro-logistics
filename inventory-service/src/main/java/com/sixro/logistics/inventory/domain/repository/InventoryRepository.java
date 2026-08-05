package com.sixro.logistics.inventory.domain.repository;

import com.sixro.logistics.inventory.domain.entity.Inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    Inventory save(Inventory inventory);

    //Optional<Inventory> findByIdAndIsDeletedFalse(UUID id);

}
