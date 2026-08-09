package com.sixro.logistics.inventory.presentation.dto.request;

import com.sixro.logistics.inventory.application.command.InventoryCheckCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InventoryCheckRequestDto(

        @NotNull(message = "허브 ID는 필수입니다.")
        UUID hubId,

        @NotEmpty(message = "상품 ID는 하나 이상 입력해야 합니다.")
        List<UUID> productIds
) {

    public InventoryCheckCommand toCommand(){
        return new InventoryCheckCommand(
                hubId,
                productIds
        );
    }

}