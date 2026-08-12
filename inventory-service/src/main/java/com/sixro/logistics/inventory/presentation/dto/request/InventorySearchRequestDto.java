package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventorySearchCommand;

import java.util.UUID;

public record InventorySearchRequestDto(
        UUID hubId,
        UUID companyId,
        UUID productId
) {

    public InventorySearchCommand toCommand(){
        return new InventorySearchCommand(
                hubId,
                companyId,
                productId
        );
    }

}
