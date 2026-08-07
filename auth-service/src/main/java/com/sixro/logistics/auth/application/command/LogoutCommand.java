package com.sixro.logistics.auth.application.command;

/**
 * 로그아웃 처리에 필요한 Access Token과 Refresh Token을 전달합니다.
 *
 * <p>두 토큰의 사용자 정보가 일치하는지 검증한 후,
 * Refresh Token 삭제와 Access Token 블랙리스트 등록에 사용됩니다.</p>
 *
 * @param accessToken  로그아웃 처리할 Access Token
 * @param refreshToken Redis에 저장된 토큰과 비교할 Refresh Token
 */
public record LogoutCommand(
        String accessToken,
        String refreshToken
) {
}