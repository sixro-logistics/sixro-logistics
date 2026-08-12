package com.sixro.logistics.order.infrastructure.client.inventory;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.port.InventoryCommandPort;
import com.sixro.logistics.order.application.port.InventoryCommandItem;
import com.sixro.logistics.order.exception.OrderErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryClientAdapter implements InventoryCommandPort {

    private final InventoryClient inventoryClient;

    @Override
    public void deductInventory(UUID hubId, List<InventoryCommandItem> items) {

        InventoryDeductRequest request = new InventoryDeductRequest(
                hubId,
                items.stream()
                        .map(item -> new InventoryRequestItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );

        try{
            inventoryClient.deductInventory(request);
        }catch(FeignException.NotFound e){
            throw new BaseException(OrderErrorCode.INVENTORY_NOT_FOUND);
        }

    }

    @Override
    public void restoreInventory(
            UUID hubId,
            List<InventoryCommandItem> items
    ) {
        InventoryRestoreRequest request = new InventoryRestoreRequest(
                hubId,
                items.stream()
                        .map(item -> new InventoryRequestItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );

        try{
            inventoryClient.restoreInventory(request);
        }catch(FeignException e){
            throw new BaseException(OrderErrorCode.INVENTORY_RESTORE_FAILED);
        }
    }

}
