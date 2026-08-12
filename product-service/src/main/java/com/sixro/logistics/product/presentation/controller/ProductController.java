package com.sixro.logistics.product.presentation.controller;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.product.application.service.ProductService;
import com.sixro.logistics.product.presentation.dto.request.ProductCreateRequestDto;
import com.sixro.logistics.product.presentation.dto.request.ProductUpdateRequestDto;
import com.sixro.logistics.product.presentation.dto.response.ProductResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Product", description = "상품 정보 조회, 상세 조회, 등록, 수정, 삭제 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Operation(
            summary ="상품 목록 조회",
            description ="모든 상품 목록 정보를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<CommonResponse<Page<ProductResponseDto>>> getProducts(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String productName,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Page<ProductResponseDto> response =
                productService.getProducts(
                        companyId,
                        productName,
                        pageable
                );

        return ResponseEntity.ok(CommonResponse.success("상품 목록 조회에 성공했습니다.", response));
    }

    /**
     * 상품 상세 조회
     */
    @Operation(
            summary ="상품 상세 조회",
            description ="(단건) 상품 상세 정보를 조회합니다."
    )
    @GetMapping("/{productId}")
    public ResponseEntity<CommonResponse<ProductResponseDto>> getCompany(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(CommonResponse.success("상품 상세 조회에 성공했습니다.", productService.getProduct(productId)));
    }

    /**
     * 상품 등록
     */
    @Operation(
            summary ="상품 등록",
            description ="신규 상품을 등록합니다."
    )
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductCreateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {

        if ("MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            // MASTER_ADMIN은 소속 정보 없이 모든 상품을 등록할 수 있습니다.
        } else if ("HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 자신의 담당 허브에만 상품을 등록할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(request.getHubId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else if("COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
            // COMPANY_MANAGER는 본인 업체에 대한 상품을 등록할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(request.getCompanyId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        ProductResponseDto response = productService.createProduct(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 상품 수정
     */
    @Operation(
            summary ="상품 수정",
            description ="기존 상품을 수정합니다."
    )
    @PatchMapping("/{productId}")
    public ResponseEntity<CommonResponse<Void>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {

        if ("MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            // MASTER_ADMIN은 소속 정보 없이 모든 상품을 등록할 수 있습니다.
        } else if ("HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 자신의 담당 허브에만 상품을 등록할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(request.getHubId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else if("COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
            // COMPANY_MANAGER는 본인 업체에 대한 상품을 등록할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(request.getCompanyId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        ProductResponseDto response =
                productService.updateProduct(
                        productId,
                        request,
                        userId
                );

        return ResponseEntity.ok(CommonResponse.success("상품이 성공적으로 수정되었습니다."));
    }

    /**
     * 상품 삭제
     */
    @Operation(
            summary ="상품 삭제",
            description ="기존 상품을 삭제합니다."
    )
    @DeleteMapping("/{productId}")
    public ResponseEntity<CommonResponse<Void>> deleteProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {

        if ("MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            // MASTER_ADMIN은 소속 정보 없이 모든 상품을 등록할 수 있습니다.
        } else if ("HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 자신의 담당 허브에만 상품을 등록할 수 있습니다.
            ProductResponseDto resCompanyDto = productService.getProduct(productId);

            if (affiliationId == null || !affiliationId.equals(resCompanyDto.getHubId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        productService.deleteProduct(
                productId,
                userId
        );

        return ResponseEntity.ok(CommonResponse.success("상품이 성공적으로 삭제되었습니다."));
    }
}
