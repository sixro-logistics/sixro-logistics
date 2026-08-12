package com.sixro.logistics.user.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * User Service의 비즈니스 오류 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "U001",
            "사용자를 찾을 수 없습니다."
    ),

    DEACTIVATED_USER(
            HttpStatus.GONE,
            "U002",
            "비활성화된 사용자입니다."
    ),

    USER_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "U003",
            "사용자 관리 권한이 없습니다."
    ),

    INVALID_AFFILIATION(
            HttpStatus.BAD_REQUEST,
            "U004",
            "유효하지 않은 소속 정보입니다."
    ),

    AFFILIATION_CHANGE_NOT_ALLOWED(
            HttpStatus.FORBIDDEN,
            "U005",
            "가입 대기 상태에서만 소속 정보를 수정할 수 있습니다."
    ),

    INVALID_APPROVE_STATUS(
            HttpStatus.CONFLICT,
            "U006",
            "가입 대기 상태의 사용자만 승인할 수 있습니다."
    ),

    INVALID_REJECT_STATUS(
            HttpStatus.CONFLICT,
            "U007",
            "가입 대기 상태의 사용자만 거절할 수 있습니다."
    ),

    SELF_DEACTIVATION_NOT_ALLOWED(
            HttpStatus.FORBIDDEN,
            "U008",
            "자신의 계정은 비활성화할 수 없습니다."
    ),

    USER_ALREADY_DEACTIVATED(
            HttpStatus.CONFLICT,
            "U009",
            "이미 비활성화된 사용자입니다."
    ),

    DUPLICATE_USERNAME(
            HttpStatus.CONFLICT,
            "U010",
            "이미 사용 중인 사용자명입니다."
    ),

    DUPLICATE_SLACK_ID(
            HttpStatus.CONFLICT,
            "U011",
            "이미 사용 중인 Slack ID입니다."
    ),

    AFFILIATION_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "U012",
            "존재하지 않거나 비활성화된 소속입니다."
    ),

    AFFILIATION_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "U013",
            "소속 정보를 확인할 수 없습니다."
    ),
    MASTER_ADMIN_SIGN_UP_NOT_ALLOWED(
            HttpStatus.FORBIDDEN,
            "U014",
            "MASTER_ADMIN은 일반 회원가입으로 생성할 수 없습니다."
    )
    ;


    private final HttpStatus status;
    private final String code;
    private final String message;
}