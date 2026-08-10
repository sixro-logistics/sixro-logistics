package com.sixro.logistics.order.application.port;

import com.sixro.logistics.order.application.model.ProductInfo;

import java.util.List;
import java.util.UUID;

public interface ProductQueryPort {

    List<ProductInfo> getProducts(List<UUID> productIds);

}
