package com.sixro.logistics.inventory.application.result;

import org.springframework.data.domain.Page;

public record InventorySearchResult(
        Page<InventorySearchItem> page
) {
}
