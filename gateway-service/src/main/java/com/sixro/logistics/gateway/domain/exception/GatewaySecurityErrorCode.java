package com.sixro.logistics.gateway.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Gateway의 인증·인가 및 보안 처리 과정에서 사용하는 오류 코드입니다.
 *
 * <p>JWT 검증, Access Token 블랙리스트,
 * 로그인 세션 검증 및 권한 확인 과정에서 발생하는 오류를 정의합니다.</p>
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

    /*
     * Access Token 블랙리스트 또는 현재 로그인 Session 등
     * Redis에 저장된 인증 상태를 조회할 수 없는 경우 사용합니다.
     *
     * 인증 상태를 확인할 수 없는 상황에서는 요청을 허용하지 않는
     * Fail Closed 정책을 적용합니다.
     */
    AUTH_STATE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "GW005",
            "인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),

    /*
     * JWT에 포함된 sessionId가 현재 Redis에 저장된 사용자 Session과
     * 일치하지 않거나 현재 로그인 Session이 존재하지 않는 경우 사용합니다.
     *
     * 새로운 로그인으로 기존 Session이 교체된 Access Token도
     * 이 오류를 통해 차단됩니다.
     */
    INVALID_SESSION(
            HttpStatus.UNAUTHORIZED,
            "GW006",
            "현재 유효한 로그인 세션이 아닙니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}