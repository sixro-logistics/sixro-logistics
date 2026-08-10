package com.sixro.logistics.auth.infrastructure.jwt;

import com.sixro.logistics.auth.domain.model.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * JWT 검증 후 애플리케이션 계층에서 사용할 인증 정보를 나타냅니다.
 *
 * <p>JJWT의 {@code Claims} 객체를 외부 계층에 노출하지 않기 위한 자료형입니다.</p>
 *
 * @param userId     토큰 소유 사용자 식별자
 * @param role       토큰 발급 시점의 사용자 권한
 * @param jwtId      토큰을 고유하게 식별하는 JWT ID
 * @param expiration 토큰 만료 시각
 */
public record JwtClaims(
        UUID userId,
        UserRole role,
        String jwtId,
        Instant expiration
) {
}