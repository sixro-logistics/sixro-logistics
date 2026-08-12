package com.sixro.logistics.order.infrastructure.client.inventory;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", path = "/api/v1/internal/inventories")
public interface InventoryClient {

    @PostMapping("/deduct")
    ResponseEntity<Void> deductInventory(
            @RequestBody InventoryDeductRequest request
    );

    @PostMapping("/restore")
    ResponseEntity<Void> restoreInventory(
            @RequestBody InventoryRestoreRequest request
    );

}
