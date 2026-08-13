package com.sixro.logistics.order.infrastructure.client.inventory;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "inventory-service", path = "/api/v1/internal/inventories")
public interface InventoryClient {

    @PostMapping("/deduct")
    ResponseEntity<Void> deductInventory(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody InventoryDeductRequest request
    );

    @PostMapping("/restore")
    ResponseEntity<Void> restoreInventory(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody InventoryRestoreRequest request
    );

}
