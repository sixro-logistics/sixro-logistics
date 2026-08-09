package com.sixro.logistics.inventory.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.*;
import com.sixro.logistics.inventory.application.result.*;
import com.sixro.logistics.inventory.domain.entity.Inventory;
import com.sixro.logistics.inventory.domain.repository.InventoryRepository;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCommandService {

    private final InventoryRepository inventoryRepository;

    public InventoryCreateResult createInventory(InventoryCreateServiceCommand command) {

        Inventory inventory = Inventory.create(
                command.hubId(),
                command.companyId(),
                command.productId(),
                command.stock()
        );

        Inventory createdInventory = inventoryRepository.save(inventory);

        return new InventoryCreateResult(
                createdInventory.getId(),
                createdInventory.getHubId(),
                createdInventory.getCompanyId(),
                createdInventory.getProductId(),
                createdInventory.getStock()
        );
    }

    public InventoryUpdateResult updateInventory(
            UUID inventoryId, InventoryUpdateCommand command
    ) {

        Inventory inventory = inventoryRepository.findByIdAndIsDeletedFalse(inventoryId)
                .orElseThrow(() ->
                        new BaseException(InventoryErrorCode.INVENTORY_NOT_FOUND)
                );

        inventory.updateStock(command.stock());

        return new InventoryUpdateResult(
                inventory.getId(), inventory.getStock()
        );
    }

    public InventoryStockInCompanyResult stockInInventory(
            UUID inventoryId, InventoryStockInCommand command
    ) {

        Inventory inventory = inventoryRepository.findByIdAndIsDeletedFalse(inventoryId)
                .orElseThrow(() ->
                        new BaseException(InventoryErrorCode.INVENTORY_NOT_FOUND)
                );

        inventory.addStock(command.quantity());

        return new InventoryStockInCompanyResult(
                inventory.getId(), inventory.getStock()
        );

    }

    public InventoryDeleteResult deleteInventory(UUID inventoryId) {

        Inventory inventory = inventoryRepository.findByIdAndIsDeletedFalse(inventoryId)
                .orElseThrow(() ->
                        new BaseException(InventoryErrorCode.INVENTORY_NOT_FOUND)
                );

        inventory.softDelete(UUID.randomUUID());

        return new InventoryDeleteResult(inventoryId);
    }

    public void deductInventory(InventoryDeductCommand command){

        Map<UUID, Integer> quantityMap = command.items().stream()
                .collect(Collectors.toMap(
                        item -> item.productId(),
                        item -> item.quantity()
                ));

        List<Inventory> inventoryList = inventoryRepository
                .findAllByHubIdAndProductIdInAndIsDeletedFalse(
                        command.hubId(),
                        command.items().stream()
                                .map(item -> item.productId())
                                .toList()
                );

        for(Inventory inventory : inventoryList){
            Integer quantity = quantityMap.get(inventory.getProductId());
            inventory.deductStock(quantity);
        }
    }
}
