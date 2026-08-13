package com.sixro.logistics.product.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.product.application.service.ProductInternalService;
import com.sixro.logistics.product.presentation.dto.response.internal.ProductInternalResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Product", description = "상품 관련 내부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/products")
public class ProductInternalController {

    private final ProductInternalService productInternalService;

    @Operation(
            summary ="상품 상세 조회(list)",
            description ="상품 목록 정보를 조회합니다."
    )
    @PostMapping("/check")
    public CommonResponse<List<ProductInternalResponseDto>> getProducts(
            @RequestBody List<UUID> productIds
    ) {
        List<ProductInternalResponseDto> response = productInternalService.getProductsByIds(productIds);
        return CommonResponse.success("상품 정보 조회가 완료되었습니다.", response);
    }

    @Operation(
            summary ="상품 상세 조회",
            description ="상품 정보를 조회합니다."
    )
    @GetMapping("/{productId}")
    public CommonResponse<ProductInternalResponseDto> getProduct(
            @PathVariable UUID productId
    ) {
        ProductInternalResponseDto response = productInternalService.getProductsById(productId);
        return CommonResponse.success("상품 정보 조회가 완료되었습니다.", response);
    }
}
