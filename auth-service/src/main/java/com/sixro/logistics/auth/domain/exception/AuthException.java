package com.sixro.logistics.auth.domain.exception;

import com.sixro.logistics.common.core.exception.BaseException;

/**
 * Auth Service의 비즈니스 규칙 위반을 나타내는 예외입니다.
 *
 * <p>{@link AuthErrorCode}를 기반으로 공통 예외 처리기에서
 * HTTP 상태와 오류 응답을 생성합니다.</p>
 */
public class AuthException extends BaseException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Feign 또는 JWT 처리 중 발생한 원인 예외를 보존하여 생성합니다.
     */
    public AuthException(
            AuthErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode, cause);
    }
}