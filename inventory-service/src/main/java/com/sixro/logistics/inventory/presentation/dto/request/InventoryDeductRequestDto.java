package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventoryDeductCommand;
import com.sixro.logistics.inventory.application.command.InventoryCommandItem;

import java.util.List;
import java.util.UUID;

public record InventoryDeductRequestDto(
        UUID idempotencyKey,
        UUID hubId,
        List<InventoryRequestItem> items
) {

    public InventoryDeductCommand toCommand(UUID idempotencyKey){
        return new InventoryDeductCommand(
                idempotencyKey,
                hubId,
                items.stream().map(item ->
                        new InventoryCommandItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );
    }

}
