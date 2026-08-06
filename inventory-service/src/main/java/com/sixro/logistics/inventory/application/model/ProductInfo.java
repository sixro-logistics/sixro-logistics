package com.sixro.logistics.inventory.application.model;

import java.util.UUID;

public record ProductInfo(
        UUID productId,
        UUID companyId
) {
}
