package com.sixro.logistics.product.application.service;


import com.sixro.logistics.product.domain.entity.Product;
import com.sixro.logistics.product.domain.repository.ProductRepository;
import com.sixro.logistics.product.presentation.dto.request.ProductCreateRequestDto;
import com.sixro.logistics.product.presentation.dto.request.ProductUpdateRequestDto;
import com.sixro.logistics.product.presentation.dto.response.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    /**
     * 상품 목록 조회
     */
    public Page<ProductResponseDto> getProducts(
            UUID companyId,
            String productName,
            Pageable pageable
    ) {

        Specification<Product> specification = isNotDeleted();

        if (companyId != null) {
            specification = specification.and(hasCompanyId(companyId));
        }

        if (productName != null && !productName.isBlank()) {
            specification = specification.and(
                    hasProductName(productName)
            );
        }

        return productRepository
                .findAll(specification, pageable)
                .map(ProductResponseDto::from);
    }

    /**
     * 상품 상세 조회
     */
    public ProductResponseDto getProduct(UUID productId) {

        Product product = productRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 상품입니다.")
                );

        return ProductResponseDto.from(product);
    }

    /**
     * 상품 등록
     */
    @Transactional
    public ProductResponseDto createProduct(
            ProductCreateRequestDto request,
            UUID userId
    ) {
        Product product = Product.builder()
                .companyId(request.getCompanyId())
                .productName(request.getProductName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();

        Product savedProduct = productRepository.save(product);

        return ProductResponseDto.from(savedProduct);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public ProductResponseDto updateProduct(
            UUID productId,
            ProductUpdateRequestDto request,
            UUID userId
    ) {

        Product product = productRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 상품입니다.")
                );

        product.updateProduct(
                request.getProductName(),
                request.getDescription(),
                request.getPrice()
        );

        return ProductResponseDto.from(product);
    }

    /**
     * 상품 논리 삭제
     */
    @Transactional
    public void deleteProduct(
            UUID productId,
            UUID userId
    ) {

        Product product = productRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 상품입니다.")
                );

        product.softDelete(userId);
    }

    private Specification<Product> isNotDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("isDeleted"));
    }

    private Specification<Product> hasCompanyId(UUID companyId) {
        return (root, query, cb) ->
                cb.equal(root.get("companyId"), companyId);
    }

    private Specification<Product> hasProductName(String productName) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("productName")),
                        "%" + productName.toLowerCase() + "%"
                );
    }
}
