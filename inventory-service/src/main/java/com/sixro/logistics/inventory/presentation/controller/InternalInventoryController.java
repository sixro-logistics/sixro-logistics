package com.sixro.logistics.inventory.presentation.controller;

import com.sixro.logistics.inventory.application.facade.InventoryCommandFacade;
import com.sixro.logistics.inventory.application.facade.InventoryQueryFacade;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryDeductRequestDto;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryRestoreRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/inventories")
public class InternalInventoryController {

    private final InventoryQueryFacade inventoryQueryFacade;
    private final InventoryCommandFacade inventoryCommandFacade;

    @PostMapping("/deduct")
    public ResponseEntity<Void> deductInventory(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody InventoryDeductRequestDto requestDto) {
        inventoryCommandFacade.deductInventory(
                requestDto.toCommand(idempotencyKey)
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restoreInventory(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody InventoryRestoreRequestDto requestDto
    ) {
        inventoryCommandFacade.restoreInventory(
                requestDto.toCommand(idempotencyKey)
        );
        return ResponseEntity.ok().build();
    }

}
