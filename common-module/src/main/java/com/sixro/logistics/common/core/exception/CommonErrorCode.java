package com.sixro.logistics.common.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 모든 서비스에서 공통으로 사용하는 HTTP 오류 코드입니다.
 *
 * <p>
 * 서비스별 비즈니스 오류(Auth, User, Gateway 등)는
 * 각 서비스의 ErrorCode(Enum)에 정의합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // ========= 400 Bad Request =========
    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "C001",
            "잘못된 요청입니다."
    ),

    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "C002",
            "입력값 검증에 실패했습니다."
    ),

    INVALID_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "C003",
            "잘못된 요청 파라미터입니다."
    ),

    // ========= 401 Unauthorized =========
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "C101",
            "인증이 필요합니다."
    ),

    // ========= 403 Forbidden =========
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "C201",
            "접근 권한이 없습니다."
    ),

    // ========= 404 Not Found =========
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "C301",
            "요청한 리소스를 찾을 수 없습니다."
    ),

    // ========= 405 Method Not Allowed =========
    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED,
            "C302",
            "지원하지 않는 HTTP 메서드입니다."
    ),

    // ========= 409 Conflict =========
    CONFLICT(
            HttpStatus.CONFLICT,
            "C401",
            "요청이 현재 리소스 상태와 충돌합니다."
    ),

    // ========= 500 Internal Server Error =========
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "C999",
            "서버 내부 오류가 발생했습니다."
    );

    /**
     * HTTP 상태 코드
     */
    private final HttpStatus status;

    /**
     * 서비스 공통 오류 코드
     */
    private final String code;

    /**
     * 사용자에게 전달할 오류 메시지
     */
    private final String message;
}
