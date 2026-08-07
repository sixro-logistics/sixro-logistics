package com.sixro.logistics.auth.domain.model;

/**
 * 한 번의 인증 과정에서 함께 발급된 Access Token과 Refresh Token입니다.
 *
 * @param accessToken  API 접근에 사용하는 단기 토큰
 * @param refreshToken 토큰 재발급에 사용하는 장기 토큰
 */
public record TokenPair(
        String accessToken,
        String refreshToken
) {
}