package com.sixro.logistics.product.presentation.dto.response;

import com.sixro.logistics.product.domain.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ProductResponseDto {

    private UUID productId;
    private UUID companyId;
    private UUID hubId;
    private String productName;
    private String description;
    private BigDecimal price;

    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
                product.getProductId(),
                product.getCompanyId(),
                product.getHubId(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice()
        );
    }
}
