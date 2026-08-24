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

    DUPLICATE_PRODUCT(
            HttpStatus.BAD_REQUEST,
            "O002",
            "동일한 상품은 하나의 주문 항목으로 합쳐서 요청해주세요."
    ),

    INVALID_DELIVERY_DEADLINE(
            HttpStatus.BAD_REQUEST,
            "O011",
            "납품기한은 미래 시각이어야 합니다."
    ),

    INVALID_REQUESTS(
            HttpStatus.BAD_REQUEST,
            "O012",
            "요청사항은 255자를 넘을 수 없습니다."
    ),

    INVALID_SORT_FIELD(
            HttpStatus.BAD_REQUEST,
            "O015",
            "생성일시, 수정일시로만 정렬할 수 있습니다."
    ),

    // 404
    ORDER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O003",
            "주문을 찾을 수 없습니다."
    ),

    HUB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O004",
            "허브를 찾을 수 없습니다."
    ),

    COMPANY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O005",
            "업체를 찾을 수 없습니다."
    ),

    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O006",
            "찾을 수 없는 상품이 포함되어 있습니다."
    ),

    INVENTORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O007",
            "재고를 찾을 수 없는 상품이 포함되어 있습니다."
    ),

    RECEIVER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "O016",
            "수령인을 찾을 수 없습니다."
    ),

    // 409
    DIFFERENT_HUB_PRODUCT(
            HttpStatus.CONFLICT,
            "O008",
            "상품들의 소속 허브가 서로 다릅니다."
    ),

    OUT_OF_STOCK(
            HttpStatus.CONFLICT,
            "O009",
            "상품의 재고가 부족합니다."
    ),

    ORDER_CANNOT_BE_MODIFIED(
            HttpStatus.CONFLICT,
            "O010",
            "해당 주문은 변경할 수 없습니다."
    ),

    INVENTORY_RESTORE_FAILED(
            HttpStatus.CONFLICT,
            "O013",
            "재고 복원에 실패했습니다."
    ),

    ORDER_CANNOT_BE_DELIVERY_CREATED(
            HttpStatus.CONFLICT,
            "O014",
        "배송 생성 상태로 변경할 수 없는 주문입니다."
    ),

    IDEMPOTENCY_KEY_CONFLICT(
            HttpStatus.CONFLICT,
            "O017",
            "동일한 멱등키로 다른 주문 요청을 보낼 수 없습니다."
    ),

    ORDER_ALREADY_PROCESSING(
            HttpStatus.CONFLICT,
            "O018",
            "동일한 멱등키의 주문 요청이 현재 처리 중입니다."
    ),

    IDEMPOTENCY_KEY_COMPENSATED(
            HttpStatus.CONFLICT,
            "O019",
            "해당 멱등키의 주문은 실패 및 보상 처리가 완료되었습니다. 새로운 멱등키로 다시 요청해주세요."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
