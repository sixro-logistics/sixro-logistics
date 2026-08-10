package com.sixro.logistics.auth.application.command;

/**
 * 토큰 재발급에 사용할 Refresh Token을 전달하는 Command입니다.
 *
 * @param refreshToken 유효성 및 Redis 저장값을 검증할 Refresh Token
 */
public record ReissueTokenCommand(
        String refreshToken
) {
}