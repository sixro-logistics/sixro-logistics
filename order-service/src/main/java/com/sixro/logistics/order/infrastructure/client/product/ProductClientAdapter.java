package com.sixro.logistics.order.infrastructure.client.product;

import com.sixro.logistics.order.application.model.ProductInfo;
import com.sixro.logistics.order.application.port.ProductQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements ProductQueryPort {

    private final ProductClient productClient;

    @Override
    public ProductInfo getProduct(UUID productId) {

        ProductClientResponse response =
                productClient.getProduct(productId);

        return null;
    }

    @Override
    public List<ProductInfo> getProducts(List<UUID> productIds) {

        ProductClientListResponse response =
                productClient.getProducts(productIds);

        return response.products().stream()
                .map(product -> new ProductInfo(
                        product.productId(),
                        product.productName(),
                        product.price(),
                        product.companyId()
                ))
                .toList();
    }
}