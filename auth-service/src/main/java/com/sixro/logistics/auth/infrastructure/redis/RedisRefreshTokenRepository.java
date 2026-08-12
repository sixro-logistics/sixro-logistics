package com.sixro.logistics.auth.infrastructure.redis;

import com.sixro.logistics.auth.domain.model.RefreshToken;
import com.sixro.logistics.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token을 Redis에 저장하는 Repository 구현체입니다.
 *
 * <p>사용자별 하나의 Refresh Token만 유지하며,
 * 새로운 로그인 또는 Token 재발급 시 기존 값은 새로운 해시값으로 교체됩니다.</p>
 *
 * <p>Refresh Token 원문은 저장하지 않고 해시값만 저장합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository
        implements RefreshTokenRepository {

    // 사용자별 Refresh Token 저장 키를 생성합니다.
    private static final String REFRESH_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    // Refresh Token 해시값을 해당 Token의 TTL과 함께 저장합니다.
    @Override
    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(
                refreshKey(refreshToken.userId()),
                refreshToken.tokenHash(),
                refreshToken.ttl()
        );
    }

    // 사용자 ID에 해당하는 현재 Refresh Token 해시값을 조회합니다.
    @Override
    public Optional<String> findTokenHashByUserId(UUID userId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(refreshKey(userId))
        );
    }

    // 사용자 ID에 해당하는 Refresh Token을 삭제합니다.
    @Override
    public void deleteByUserId(UUID userId) {
        redisTemplate.delete(refreshKey(userId));
    }

    private String refreshKey(UUID userId) {
        return REFRESH_PREFIX + userId;
    }
}