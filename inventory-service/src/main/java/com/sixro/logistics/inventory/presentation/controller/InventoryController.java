package com.sixro.logistics.inventory.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.application.facade.InventoryCommandFacade;
import com.sixro.logistics.inventory.application.facade.InventoryQueryFacade;
import com.sixro.logistics.inventory.application.result.*;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCreateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.request.InventorySearchRequestDto;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryStockInRequestDto;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryUpdateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventories")
public class InventoryController {

    private final InventoryCommandFacade inventoryCommandFacade;
    private final InventoryQueryFacade inventoryQueryFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<InventoryCreateResponseDto>> createInventory(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @Valid @RequestBody InventoryCreateRequestDto requestDto) {

        InventoryCreateResult result =
                inventoryCommandFacade.createInventory(
                        userRole, affiliationId, requestDto.toCommand()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED,
                        "재고가 등록되었습니다.",
                        InventoryCreateResponseDto.from(result)
                ));
    }

    @GetMapping("/{inventoryId}")
    public ResponseEntity<CommonResponse<InventoryGetOneResponseDto>> getOneInventory(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID inventoryId) {

        InventoryGetOneResult result =
                inventoryQueryFacade.getOneInventory(
                        userRole, affiliationId, inventoryId
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "재고 조회에 성공했습니다.",
                        InventoryGetOneResponseDto.from(result)
                ));
    }

    @PatchMapping("/{inventoryId}")
    public ResponseEntity<CommonResponse<InventoryUpdateResponseDto>> updateInventory(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId,
            @PathVariable UUID inventoryId,
            @Valid @RequestBody InventoryUpdateRequestDto requestDto
    ) {

        InventoryUpdateResult result =
                inventoryCommandFacade.updateInventory(
                        userRole, affiliationId, inventoryId, requestDto.toCommand()
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "재고가 수정되었습니다.",
                        InventoryUpdateResponseDto.from(result)
                ));
    }

    @PatchMapping("/{inventoryId}/stock")
    public ResponseEntity<CommonResponse<InventoryStockInResponseDto>> stockInInventory(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId,
            @PathVariable UUID inventoryId,
            @Valid @RequestBody InventoryStockInRequestDto requestDto
    ) {

        InventoryStockInCompanyResult result =
                inventoryCommandFacade.stockInInventory(
                        userRole, affiliationId, inventoryId, requestDto.toCommand()
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "재고가 입고되었습니다.",
                        InventoryStockInResponseDto.from(result)
                ));
    }

    @DeleteMapping("/{inventoryId}")
    public ResponseEntity<CommonResponse<InventoryDeleteResponseDto>> deleteInventory(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId,
            @PathVariable UUID inventoryId
    ) {

        InventoryDeleteResult result =
                inventoryCommandFacade.deleteInventory(
                        userId, userRole, affiliationId, inventoryId
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "재고가 삭제되었습니다.",
                        InventoryDeleteResponseDto.from(result)
                ));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<InventorySearchResponseDto>>> searchInventory(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId,
            @ModelAttribute InventorySearchRequestDto requestDto,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC,
                    size = 10
            ) Pageable pageable
    ) {

        InventorySearchResult result =
                inventoryQueryFacade.searchInventory(
                        userRole, affiliationId, requestDto.toCommand(), pageable
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "재고 목록 조회에 성공했습니다.",
                        PageResponse.from(
                                result.page(),
                                item -> new InventorySearchResponseDto(
                                        item.inventoryId(),
                                        item.hubId(),
                                        item.companyId(),
                                        item.productId(),
                                        item.stock()
                                )
                        )
                ));
    }

}
