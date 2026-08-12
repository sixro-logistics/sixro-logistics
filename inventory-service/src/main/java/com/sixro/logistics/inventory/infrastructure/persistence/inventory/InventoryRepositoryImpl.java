package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.sixro.logistics.inventory.application.command.InventorySearchCommand;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public Optional<Inventory> findByIdAndIsDeletedFalse(UUID inventoryId) {
        return inventoryJpaRepository.findByIdAndIsDeletedFalse(inventoryId);
    }

    @Override
    public Optional<Inventory> findForUpdateByIdAndIsDeletedFalse(UUID inventoryId) {
        return inventoryJpaRepository.findForUpdateByIdAndIsDeletedFalse(inventoryId);
    }

    @Override
    public List<Inventory> findAllByHubIdAndProductIdInAndIsDeletedFalse(UUID hubId, List<UUID> productsId) {
        return inventoryJpaRepository.findAllByHubIdAndProductIdInAndIsDeletedFalse(hubId, productsId);
    }

    @Override
    public List<Inventory> findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(UUID hubId, List<UUID> productsId) {
        return inventoryJpaRepository.findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(hubId, productsId);
    }

    @Override
    public Page<Inventory> findAll(
            UserRole userRole, UUID affiliationId,
            InventorySearchCommand command, Pageable pageable
    ) {
        return inventoryQueryRepository.findAll(userRole, affiliationId, command, pageable);
    }

}
