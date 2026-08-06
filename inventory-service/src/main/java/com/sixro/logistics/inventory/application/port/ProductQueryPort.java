package com.sixro.logistics.inventory.application.port;

import com.sixro.logistics.inventory.application.model.ProductInfo;
import java.util.UUID;

public interface ProductQueryPort {

    ProductInfo getProduct(UUID productID);

}
