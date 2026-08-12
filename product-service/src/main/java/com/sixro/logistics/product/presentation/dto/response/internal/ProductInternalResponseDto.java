package com.sixro.logistics.product.presentation.dto.response.internal;

import com.sixro.logistics.product.domain.entity.Product;
import java.util.UUID;

public record ProductInternalResponseDto(
        UUID productId,
        String productName,
        Integer price,
        UUID companyId
) {
    public static ProductInternalResponseDto from(Product product) {
        return new ProductInternalResponseDto(
                product.getProductId(),
                product.getProductName(),
                product.getPrice() != null ? product.getPrice().intValue() : 0,
                product.getCompanyId()
        );
    }
}
