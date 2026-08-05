package com.sixro.logistics.inventory.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.inventory.application.facade.InventoryFacade;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCreateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.InventoryCreateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InventoryController {

    private final InventoryFacade inventoryFacade;

    @PostMapping("/inventories")
    public ResponseEntity<CommonResponse<InventoryCreateResponseDto>> createInventory(
            @Valid @RequestBody InventoryCreateRequestDto requestDto) {

        InventoryCreateResponseDto responseDto =
                inventoryFacade.createInventory(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED,
                        "재고가 등록되었습니다.",
                        responseDto
                ));
    }

}
