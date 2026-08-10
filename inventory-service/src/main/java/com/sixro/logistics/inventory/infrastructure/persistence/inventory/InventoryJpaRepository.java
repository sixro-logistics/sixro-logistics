package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryJpaRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByIdAndIsDeletedFalse(UUID inventoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findForUpdateByIdAndIsDeletedFalse(UUID inventoryId);

    List<Inventory> findAllByHubIdAndProductIdInAndIsDeletedFalse(UUID hubId, List<UUID> productsId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAllForDeductByHubIdAndProductIdInAndIsDeletedFalse(UUID hubId, List<UUID> productsid);

}
