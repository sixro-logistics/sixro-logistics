package com.sixro.logistics.inventory.presentation.controller;

import com.sixro.logistics.inventory.application.facade.InventoryQueryFacade;
import com.sixro.logistics.inventory.application.result.InventoryCheckResult;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCheckRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.InventoryCheckResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/inventories")
public class InternalInventoryController {

    private final InventoryQueryFacade inventoryQueryFacade;

    @PostMapping("/check")
    public ResponseEntity<InventoryCheckResponseDto> createInventory(
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,*/
            @Valid @RequestBody InventoryCheckRequestDto requestDto) {

        InventoryCheckResult checkResult
                = inventoryQueryFacade.checkInventory(requestDto.toCommand());

        return ResponseEntity
                .ok()
                .body(InventoryCheckResponseDto.from(checkResult));
    }

}
