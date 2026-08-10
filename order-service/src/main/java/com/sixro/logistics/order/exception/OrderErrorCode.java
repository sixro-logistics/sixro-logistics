package com.sixro.logistics.order.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    // 400
    INVALID_QUANTITY(
            HttpStatus.BAD_REQUEST,
            "O001",
            "주문 수량은 1 이상이어야 합니다."
    ),

    // 404
    HUB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O003",
            "허브를 찾을 수 없습니다."
    ),

    COMPANY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O004",
            "업체를 찾을 수 없습니다."
    ),

    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O005",
            "찾을 수 없는 상품이 포함되어 있습니다."
    ),

    INVENTORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O006",
            "재고가 등록되지 않은 상품이 포함되어 있습니다."
    ),

    // 409
    DIFFERENT_HUB_PRODUCT(
            HttpStatus.CONFLICT,
            "O007",
            "상품들의 소속 허브가 서로 다릅니다."
    ),

    OUT_OF_STOCK(
            HttpStatus.CONFLICT,
            "O008",
            "상품의 재고가 부족합니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
