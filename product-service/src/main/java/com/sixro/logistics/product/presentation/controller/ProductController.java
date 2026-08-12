package com.sixro.logistics.product.presentation.controller;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.product.application.service.ProductService;
import com.sixro.logistics.product.presentation.dto.request.ProductCreateRequestDto;
import com.sixro.logistics.product.presentation.dto.request.ProductUpdateRequestDto;
import com.sixro.logistics.product.presentation.dto.response.ProductResponseDto;
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

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    /**
     * 상품 목록 조회
     * GET /products
     * GET /products?companyId=UUID
     * GET /products?productName=키보드
     * GET /products?page=0&size=10
     */
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
    @GetMapping("/{productId}")
    public ResponseEntity<CommonResponse<ProductResponseDto>> getCompany(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(CommonResponse.success("상품 목록 조회에 성공했습니다.", productService.getProduct(productId)));
    }

    /**
     * 상품 등록
     */
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductCreateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {
        // 권한 검증: MASTER 또는 HUB_ADMIN 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole) && !"HUB_ADMIN".equalsIgnoreCase(userRole) && !"COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 본인 허브에 대한 상품만 등록 가능
            if("HUB_ADMIN".equalsIgnoreCase(userRole)) {
                if (!affiliationId.equals(request.getHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }

            // COMPANY_MANAGER 본인 상품에 대한 상품만 등록 가능
            if("COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
                if (!affiliationId.equals(request.getCompanyId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        ProductResponseDto response =
                productService.createProduct(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 상품 수정
     */
    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {
        // 권한 검증: MASTER 또는 HUB_MANAGER가 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole) && !"HUB_ADMIN".equalsIgnoreCase(userRole) && !"COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
            if("HUB_ADMIN".equalsIgnoreCase(userRole)) {
                // HUB_ADMIN은 본인 허브에 대한 상품만 수정 가능
                if (!affiliationId.equals(request.getHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            } else if("COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
                // COMPANY_MANAGER는 본인 상품에 대한 상품만 수정 가능
                if (!affiliationId.equals(request.getCompanyId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        ProductResponseDto response =
                productService.updateProduct(
                        productId,
                        request,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<CommonResponse<Void>> deleteProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {
        // 권한 검증: MASTER 또는 HUB_MANAGER가 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole) && !"HUB_ADMIN".equalsIgnoreCase(userRole)) {
            if("HUB_ADMIN".equalsIgnoreCase(userRole)) {
                ProductResponseDto resCompanyDto = productService.getProduct(productId);
                // HUB_ADMIN은 본인 허브에 대한 상품만 삭제 가능
                if (!affiliationId.equals(resCompanyDto.getHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        productService.deleteProduct(
                productId,
                userId
        );

        return ResponseEntity.ok(CommonResponse.success("상품가 성공적으로 삭제되었습니다."));
    }
}
