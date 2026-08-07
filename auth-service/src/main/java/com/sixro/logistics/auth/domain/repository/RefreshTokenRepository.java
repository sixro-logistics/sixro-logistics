package com.sixro.logistics.auth.domain.repository;

import com.sixro.logistics.auth.domain.model.RefreshToken;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token과 Access Token 블랙리스트 저장소에 대한 추상화입니다.
 *
 * <p>애플리케이션 계층이 Redis 구현에 직접 의존하지 않도록
 * 저장소 계약만 도메인 계층에 정의합니다.</p>
 */
public interface RefreshTokenRepository {

    // 사용자의 Refresh Token 해시값을 만료 시간과 함께 저장합니다.
    void save(RefreshToken refreshToken);

    // 사용자 ID로 저장된 Refresh Token 해시값을 조회합니다.
    Optional<String> findTokenHashByUserId(UUID userId);

    // 사용자 ID에 해당하는 Refresh Token을 삭제합니다.
    void deleteByUserId(UUID userId);

    // 로그아웃한 Access Token의 JWT ID를 남은 유효 시간 동안 저장합니다.
    void blacklistAccessToken(
            String jwtId,
            Duration ttl
    );

    boolean isBlacklisted(String jwtId);
}