package com.sixro.logistics.delivery.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeliveryErrorCode implements ErrorCode {

    INVALID_DELIVERY_MANAGER_TYPE(
            HttpStatus.BAD_REQUEST,
            "D001",
            "유효하지 않은 배송 담당자 유형입니다."
    ),

    DELIVERY_MANAGER_TYPE_HUB_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "D002",
            "담당자 유형과 소속 허브 정보가 일치하지 않습니다."
    ),

    USER_NOT_ELIGIBLE_FOR_DELIVERY_MANAGER(
            HttpStatus.BAD_REQUEST,
            "D003",
            "배송 담당자로 등록할 수 없는 사용자입니다."
    ),

    DELIVERY_MANAGER_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "D004",
            "배송 담당자 관리 권한이 없습니다."
    ),

    DELIVERY_MANAGER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "D005",
            "이미 등록된 배송 담당자입니다."
    ),

    DELIVERY_MANAGER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "D006",
            "배송 담당자를 찾을 수 없습니다."
    ),

    DELIVERY_MANAGER_CAPACITY_EXCEEDED(
            HttpStatus.CONFLICT,
            "D009",
            "등록할 수 있는 배송 담당자 수를 초과했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
