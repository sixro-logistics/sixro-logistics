package com.sixro.logistics.auth.domain.model;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis에 저장할 Refresh Token 정보를 나타냅니다.
 *
 * <p>보안을 위해 Refresh Token 원문이 아닌 해시값만 저장합니다.</p>
 *
 * @param userId    토큰 소유 사용자 식별자
 * @param tokenHash Refresh Token 해시값
 * @param ttl       Redis 저장 만료 시간
 */
public record RefreshToken(
        UUID userId,
        String tokenHash,
        Duration ttl
) {
}