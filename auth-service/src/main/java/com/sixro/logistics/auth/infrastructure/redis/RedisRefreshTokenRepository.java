package com.sixro.logistics.auth.infrastructure.redis;

import com.sixro.logistics.auth.domain.model.RefreshToken;
import com.sixro.logistics.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token과 로그아웃 Access Token을 Redis에 저장하는 Repository 구현체입니다.
 *
 * <p>Refresh Token은 사용자 ID를 키로 하여 해시값만 저장하고,
 * 로그아웃한 Access Token은 JWT ID를 키로 남은 유효 시간 동안 저장합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository
        implements RefreshTokenRepository {

    // 사용자별 Refresh Token 저장 키를 생성합니다.
    private static final String REFRESH_PREFIX = "refresh:";
    // 로그아웃 Access Token의 블랙리스트 키를 생성합니다.
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(
                refreshKey(refreshToken.userId()),
                refreshToken.tokenHash(),
                refreshToken.ttl()
        );
    }

    @Override
    public Optional<String> findTokenHashByUserId(UUID userId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(refreshKey(userId))
        );
    }

    @Override
    public void deleteByUserId(UUID userId) {
        redisTemplate.delete(refreshKey(userId));
    }

    @Override
    public void blacklistAccessToken(
            String jwtId,
            Duration ttl
    ) {
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jwtId,
                "logout",
                ttl
        );
    }

    @Override
    public boolean isBlacklisted(String jwtId) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + jwtId);
    }

    private String refreshKey(UUID userId) {
        return REFRESH_PREFIX + userId;
    }
}