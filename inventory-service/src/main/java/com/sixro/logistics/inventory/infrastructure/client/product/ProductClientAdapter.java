package com.sixro.logistics.inventory.infrastructure.client.product;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.inventory.application.model.ProductInfo;
import com.sixro.logistics.inventory.application.port.ProductQueryPort;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements ProductQueryPort {

    private final ProductClient productClient;

    @Override
    public ProductInfo getProduct(UUID productID) {

        try {
            CommonResponse<ProductClientResponse> response = productClient.getProduct(productID);

            return new ProductInfo(
                    response.data().productId(),
                    response.data().companyId()
            );

        } catch (FeignException.NotFound e) {
            throw new BaseException(InventoryErrorCode.PRODUCT_NOT_FOUND);
        }

    }

}
