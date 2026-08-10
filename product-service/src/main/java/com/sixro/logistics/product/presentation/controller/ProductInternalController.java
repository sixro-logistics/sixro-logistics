package com.sixro.logistics.product.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.product.application.service.ProductInternalService;
import com.sixro.logistics.product.presentation.dto.response.internal.ProductInternalResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/products")
public class ProductInternalController {

    private final ProductInternalService productInternalService;

    /**
     * 상품 상세 조회
     */
    @PostMapping("/check")
    public CommonResponse<List<ProductInternalResponseDto>> getProducts(
            @RequestBody List<UUID> productIds
    ) {
        List<ProductInternalResponseDto> response = productInternalService.getProductsByIds(productIds);
        return CommonResponse.success("상품 정보 조회가 완료되었습니다.", response);
    }
}
