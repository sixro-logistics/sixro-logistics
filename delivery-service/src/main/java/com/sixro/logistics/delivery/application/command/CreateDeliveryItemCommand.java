package com.sixro.logistics.delivery.application.command;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CreateDeliveryItemCommand {

    private final UUID companyId;
    private final UUID productId;
    private final Integer quantity;

    public CreateDeliveryItemCommand(UUID companyId, UUID productId, Integer quantity) {
        this.companyId = companyId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
