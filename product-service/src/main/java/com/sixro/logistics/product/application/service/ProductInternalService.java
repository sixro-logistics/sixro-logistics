package com.sixro.logistics.product.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.product.domain.entity.Product;
import com.sixro.logistics.product.domain.repository.internal.ProductInternalRepository;
import com.sixro.logistics.product.exception.ProductErrorCode;
import com.sixro.logistics.product.presentation.dto.response.ProductResponseDto;
import com.sixro.logistics.product.presentation.dto.response.internal.ProductInternalResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductInternalService {

    private final ProductInternalRepository productInternalRepository;


    /**
     * 상품 존재 유무 확인(list)
     */
    public List<ProductInternalResponseDto> getProductsByIds(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<Product> products = productInternalRepository.findAllByProductIdInAndIsDeletedFalse(productIds);

        // 요청한 상품 중 존재하지 않거나 삭제된 상품이 있는 경우 예외 처리 (선택 사항)
        if (products.size() != productIds.size()) {
            throw new BaseException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        return products.stream()
                .map(ProductInternalResponseDto::from)
                .toList();
    }

    /**
     * 상품 상세 조회
     */
    public ProductInternalResponseDto getProductsById(UUID productId) {
        Product product = productInternalRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 상품입니다.")
                );

        return ProductInternalResponseDto.from(product);
    }
}
