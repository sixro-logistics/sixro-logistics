package com.sixro.logistics.auth.infrastructure.redis;

import com.sixro.logistics.auth.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자별 현재 로그인 세션을 Redis에 저장하는 Repository 구현체입니다.
 *
 * <p>사용자당 하나의 sessionId만 저장하므로
 * 새로운 로그인 시 기존 세션은 자동으로 교체됩니다.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSessionRepository
        implements SessionRepository {

    private static final String SESSION_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(
            UUID userId,
            UUID sessionId,
            Duration ttl
    ) {
        redisTemplate.opsForValue().set(
                sessionKey(userId),
                sessionId.toString(),
                ttl
        );
    }

    @Override
    public Optional<UUID> findSessionIdByUserId(
            UUID userId
    ) {
        String sessionId =
                redisTemplate.opsForValue()
                        .get(sessionKey(userId));

        if (sessionId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    UUID.fromString(sessionId)
            );
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Invalid sessionId stored in Redis. userId={}, sessionId={}",
                    userId,
                    sessionId
            );

            return Optional.empty();
        }
    }

    @Override
    public void deleteByUserId(UUID userId) {
        redisTemplate.delete(sessionKey(userId));
    }

    private String sessionKey(UUID userId) {
        return SESSION_PREFIX + userId;
    }
}