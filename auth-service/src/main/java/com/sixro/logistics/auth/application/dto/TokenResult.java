package com.sixro.logistics.auth.application.dto;

/**
 * 로그인 또는 토큰 재발급 결과를 Presentation 계층으로 전달합니다.
 *
 * @param tokenType            인증 헤더에 사용할 토큰 타입
 * @param accessToken          API 접근에 사용할 Access Token
 * @param refreshToken         토큰 재발급에 사용할 Refresh Token
 * @param accessTokenExpiresIn Access Token의 유효 시간(초)
 */
public record TokenResult(
        String tokenType,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {

    public static TokenResult bearer(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn
    ) {
        return new TokenResult(
                "Bearer",
                accessToken,
                refreshToken,
                accessTokenExpiresIn
        );
    }
}