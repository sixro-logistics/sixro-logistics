package com.sixro.logistics.auth.presentation.response;

import com.sixro.logistics.auth.application.dto.TokenResult;

/**
 * 로그인 또는 토큰 재발급 성공 시 반환하는 토큰 응답입니다.
 *
 * @param tokenType            인증 방식
 * @param accessToken          API 접근용 Access Token
 * @param refreshToken         토큰 재발급용 Refresh Token
 * @param accessTokenExpiresIn Access Token의 유효 시간(초)
 */
public record TokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {

    /**
     * 애플리케이션 계층의 토큰 발급 결과를 API 응답으로 변환합니다.
     */
    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(
                result.tokenType(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresIn()
        );
    }
}