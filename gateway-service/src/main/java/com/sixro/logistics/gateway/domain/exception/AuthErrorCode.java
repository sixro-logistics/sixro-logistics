package com.sixro.logistics.gateway.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Gateway 인증·인가 과정에서 사용하는 오류 코드입니다.
 *
 * <p>JWT 검증, Access Token 상태 확인, 권한 검사 과정에서
 * 발생하는 예외를 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    /**
     * JWT 형식이 올바르지 않거나
     * 필수 Claim이 존재하지 않는 경우
     */
    INVALID_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A008",
            "유효하지 않은 Access Token입니다."
    ),

    /**
     * Access Token의 만료 시간이 지난 경우
     */
    EXPIRED_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A009",
            "만료된 Access Token입니다."
    ),

    /**
     * Redis 블랙리스트에 등록된 Access Token인 경우
     */
    BLACKLISTED_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "A010",
            "이미 로그아웃된 토큰입니다."
    ),

    /**
     * 인증은 완료되었지만
     * 요청한 리소스에 대한 권한이 없는 경우
     */
    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "A015",
            "접근 권한이 없습니다."
    );


    private final HttpStatus status;
    private final String code;
    private final String message;
}
