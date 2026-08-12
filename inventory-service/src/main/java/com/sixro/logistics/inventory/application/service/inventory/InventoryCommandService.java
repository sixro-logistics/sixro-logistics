package com.sixro.logistics.inventory.application.service.inventory;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.*;
import com.sixro.logistics.inventory.application.result.*;
import com.sixro.logistics.inventory.application.service.event.ProcessedEventService;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryRepository;
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
    private final ProcessedEventService processedEventService;

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

        Inventory inventory = inventoryRepository.findForUpdateByIdAndIsDeletedFalse(inventoryId)
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

        Inventory inventory = inventoryRepository.findForUpdateByIdAndIsDeletedFalse(inventoryId)
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

    public void deductInventory(InventoryDeductCommand command) {

        Map<UUID, Integer> quantityMap = command.items()
                .stream()
                .collect(Collectors.toMap(
                        item -> item.productId(),
                        item -> item.quantity()
                ));

        List<UUID> productIds = command.items()
                .stream()
                .map(item -> item.productId())
                .toList();

        List<Inventory> inventoryList =
                inventoryRepository
                        .findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(
                                command.hubId(),
                                productIds
                        );

        // 요청한 상품 중 재고가 존재하지 않는 상품이 있는지 검증
        if(inventoryList.size() != productIds.size()){
            throw new BaseException(InventoryErrorCode.INVENTORY_NOT_FOUND);
        }

        for(Inventory inventory : inventoryList){
            Integer quantity = quantityMap.get(inventory.getProductId());

            inventory.deductStock(quantity);
        }

    }

    public void restoreInventory(InventoryRestoreCommand command) {
        restoreStock(command.hubId(), command.items());
    }

    public void restoreInventoryByEvent(InventoryRestoreByEventCommand command) {
        // 이미 소비한 이벤트
        if(processedEventService.isProcessed(command.eventId())){
            return;
        }

        restoreStock(command.hubId(), command.items());
        processedEventService.save(command.eventId());
    }

    private void restoreStock(
            UUID hubId,
            List<InventoryCommandItem> items
    ) {

        Map<UUID, Integer> quantityMap = items.stream()
                .collect(Collectors.toMap(
                        InventoryCommandItem::productId,
                        InventoryCommandItem::quantity
                ));

        List<UUID> productIds = items.stream()
                .map(InventoryCommandItem::productId)
                .toList();

        List<Inventory> inventoryList =
                inventoryRepository
                        .findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(
                                hubId,
                                productIds
                        );

        if(inventoryList.size() != productIds.size()){
            throw new BaseException(
                    InventoryErrorCode.INVENTORY_NOT_FOUND
            );
        }

        for(Inventory inventory : inventoryList){
            Integer quantity = quantityMap.get(inventory.getProductId());

            inventory.restoreStock(quantity);
        }
    }

}
