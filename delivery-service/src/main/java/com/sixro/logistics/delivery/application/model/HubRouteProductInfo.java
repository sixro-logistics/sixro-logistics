package com.sixro.logistics.delivery.application.model;

import java.util.UUID;

// 허브 경로 조회에 전달할 주문 상품 정보
public record HubRouteProductInfo(
        UUID productId,
        Integer quantity
) {
}
