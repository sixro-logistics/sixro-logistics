package com.sixro.logistics.auth.infrastructure.redis;

import com.sixro.logistics.auth.domain.repository.AccessTokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 무효화된 Access Token의 JWT ID를 Redis에 저장하는 Repository 구현체입니다.
 *
 * <p>로그아웃한 Access Token은 JWT ID를 키로 남은 유효 시간 동안 저장합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisAccessTokenBlacklistRepository
        implements AccessTokenBlacklistRepository {

    // 로그아웃 Access Token의 블랙리스트 키를 생성합니다.
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(
            String jwtId,
            Duration ttl
    ) {
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        redisTemplate.opsForValue().set(
                blacklistKey(jwtId),
                "logout",
                ttl
        );
    }

    @Override
    public boolean exists(String jwtId) {
        return redisTemplate.hasKey(
                blacklistKey(jwtId)
        );
    }

    private String blacklistKey(String jwtId) {
        return BLACKLIST_PREFIX + jwtId;
    }
}