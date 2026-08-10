package com.sixro.logistics.gateway.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Gateway의 인증·인가 및 보안 처리 과정에서 사용하는 오류 코드입니다.
 *
 * <p>JWT 검증, Access Token 상태 확인, 권한 검사 및
 * 인증 인프라 장애로 발생하는 오류를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum GatewaySecurityErrorCode implements ErrorCode {

    INVALID_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "GW001",
            "유효하지 않은 Access Token입니다."
    ),

    EXPIRED_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "GW002",
            "만료된 Access Token입니다."
    ),

    BLACKLISTED_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "GW003",
            "이미 로그아웃된 토큰입니다."
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "GW004",
            "접근 권한이 없습니다."
    ),

    TOKEN_BLACKLIST_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "GW005",
            "인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}