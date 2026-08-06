package com.sixro.logistics.inventory.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InventoryErrorCode implements ErrorCode {

    // 400
    INVALID_FILTER(
            HttpStatus.BAD_REQUEST,
            "I006",
            "허브 ID, 상품 ID로만 필터링할 수 있습니다."
    ),

    INVALID_SORT_FIELD(
            HttpStatus.BAD_REQUEST,
            "I007",
            "생성일시, 수정일시로만 정렬할 수 있습니다."
    ),

    INVALID_SORT_DIRECTION(
            HttpStatus.BAD_REQUEST,
            "I008",
            "정렬 방향은 asc, desc만 가능합니다."
    ),

    INVALID_QUANTITY(
            HttpStatus.BAD_REQUEST,
            "I009",
            "수량은 1 이상이어야 합니다."
    ),

    // 403
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "I001",
            "재고 관련 권한이 없습니다."
    ),

    // 404
    HUB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "I002",
            "허브를 찾을 수 없습니다."
    ),

    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "I003",
            "상품을 찾을 수 없습니다."
    ),

    INVENTORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "I004",
            "재고를 찾을 수 없습니다."
    ),

    // 409
    OUT_OF_STOCK(
            HttpStatus.CONFLICT,
            "I005",
            "상품의 재고가 부족합니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}