package com.sixro.logistics.inventory.application.service.inventory;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.InventorySearchCommand;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.application.result.*;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryRepository;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;
    
    public InventoryGetOneResult getOneInventory(UUID inventoryId) {

        Inventory inventory = inventoryRepository.findByIdAndIsDeletedFalse(inventoryId)
                .orElseThrow(() ->
                        new BaseException(InventoryErrorCode.INVENTORY_NOT_FOUND)
                );

        return new InventoryGetOneResult(
                inventory.getId(),
                inventory.getHubId(),
                inventory.getCompanyId(),
                inventory.getProductId(),
                inventory.getStock()
        );

    }

    public InventorySearchResult searchInventory(
            UserRole userRole, UUID affiliationId,
            InventorySearchCommand command, Pageable pageable
    ) {

        Page<Inventory> inventoryPage
                = inventoryRepository.findAll(userRole, affiliationId, command, pageable);

        Page<InventorySearchItem> resultPage =
                inventoryPage.map(inventory ->
                        new InventorySearchItem(
                                inventory.getId(),
                                inventory.getHubId(),
                                inventory.getCompanyId(),
                                inventory.getProductId(),
                                inventory.getStock()
                        )
                );

        return new InventorySearchResult(resultPage);
    }
}
