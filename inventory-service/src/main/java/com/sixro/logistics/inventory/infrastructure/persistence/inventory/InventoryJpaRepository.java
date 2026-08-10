package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryJpaRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByIdAndIsDeletedFalse(UUID inventoryId);

    List<Inventory> findAllByHubIdAndProductIdInAndIsDeletedFalse(UUID hubId, List<UUID> productsId);

}
