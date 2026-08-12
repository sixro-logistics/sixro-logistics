package com.sixro.logistics.inventory.presentation.controller;

import com.sixro.logistics.inventory.application.facade.InventoryCommandFacade;
import com.sixro.logistics.inventory.application.facade.InventoryQueryFacade;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryDeductRequestDto;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryRestoreRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/inventories")
public class InternalInventoryController {

    private final InventoryQueryFacade inventoryQueryFacade;
    private final InventoryCommandFacade inventoryCommandFacade;

    @PostMapping("/deduct")
    public ResponseEntity<Void> deductInventory(
            @Valid @RequestBody InventoryDeductRequestDto requestDto) {
        inventoryCommandFacade.deductInventory(requestDto.toCommand());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restoreInventory(
            @Valid @RequestBody InventoryRestoreRequestDto requestDto
    ) {
        inventoryCommandFacade.restoreInventory(requestDto.toCommand());
        return ResponseEntity.ok().build();
    }

}
