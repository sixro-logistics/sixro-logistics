package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventoryCommandItem;
import com.sixro.logistics.inventory.application.command.InventoryRestoreCommand;

import java.util.List;
import java.util.UUID;

public record InventoryRestoreRequestDto(
        UUID hubId,
        List<InventoryRequestItem> items
) {

    public InventoryRestoreCommand toCommand() {
        return new InventoryRestoreCommand(
                hubId,
                items.stream()
                        .map(item ->
                                new InventoryCommandItem(
                                    item.productId(),
                                    item.quantity()
                        ))
                        .toList()
        );
    }

}
