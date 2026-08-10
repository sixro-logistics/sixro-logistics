package com.sixro.logistics.inventory.infrastructure.client.product;

import java.util.UUID;

public record ProductClientResponse(
        UUID productId,
        UUID companyId
) {
}
