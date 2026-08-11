package com.sixro.logistics.inventory.domain.repository.inventory;

import com.sixro.logistics.inventory.application.command.InventorySearchCommand;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByIdAndIsDeletedFalse(UUID id);

    Optional<Inventory> findForUpdateByIdAndIsDeletedFalse(UUID inventoryId);

    List<Inventory> findAllByHubIdAndProductIdInAndIsDeletedFalse(
            UUID hubId, List<UUID> productsId
    );

    List<Inventory> findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(
            UUID hubId, List<UUID> productsId
    );

    Page<Inventory> findAll(
            UserRole userRole, UUID affiliationId,
            InventorySearchCommand command, Pageable pageable
    );

}
