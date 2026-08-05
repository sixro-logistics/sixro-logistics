package com.sixro.logistics.inventory.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.inventory.application.facade.InventoryFacade;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCreateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.InventoryCreateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InventoryController {

    private final InventoryFacade inventoryFacade;

    @PostMapping("/inventories")
    public CommonResponse<InventoryCreateResponseDto> createInventory(
            @Valid @RequestBody InventoryCreateRequestDto requestDto) {

        InventoryCreateResponseDto responseDto =
                inventoryFacade.createInventory(requestDto);

        return CommonResponse.created(
                "재고가 등록되었습니다.",
                responseDto
        );
    }

}
