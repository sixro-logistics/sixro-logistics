package com.sixro.logistics.auth.infrastructure.redis;

import com.sixro.logistics.auth.domain.repository.AuthStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Login / Logout 과정의 여러 Redis 상태 변경을
 * Lua Script로 원자적으로 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class RedisAuthStateRepository
        implements AuthStateRepository {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String SESSION_PREFIX = "session:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private static final DefaultRedisScript<Long> LOGIN_SCRIPT =
            createScript("redis/login.lua");

    private static final DefaultRedisScript<Long> LOGOUT_SCRIPT =
            createScript("redis/logout.lua");

    private static final DefaultRedisScript<Long> REISSUE_SCRIPT =
            createScript("redis/reissue.lua");

    private final StringRedisTemplate redisTemplate;

    /**
     * 로그인 시 Refresh Token과 Session을 함께 저장합니다.
     */
    @Override
    public void saveLoginState(
            UUID userId,
            String refreshTokenHash,
            UUID sessionId,
            Duration ttl
    ) {
        Long result = redisTemplate.execute(
                LOGIN_SCRIPT,
                List.of(
                        refreshKey(userId),
                        sessionKey(userId)
                ),
                refreshTokenHash,
                sessionId.toString(),
                String.valueOf(ttl.toMillis())
        );

        validateScriptResult(result);
    }

    /**
     * 로그아웃 시 Refresh Token과 Session을 삭제하고,
     * 유효한 Access Token이 있다면 blacklist에 등록합니다.
     */
    @Override
    public void clearLoginState(
            UUID userId,
            String accessTokenJwtId,
            Duration accessTokenTtl
    ) {
        String jwtId =
                accessTokenJwtId == null
                        ? ""
                        : accessTokenJwtId;

        long ttlMillis =
                accessTokenTtl == null
                        ? 0L
                        : Math.max(
                        accessTokenTtl.toMillis(),
                        0L
                );

        /*
         * Access Token이 만료된 경우 Lua에서 blacklist 저장을 생략하지만
         * KEYS[3] 인자는 항상 전달해야 하므로 사용하지 않을 placeholder key를 전달합니다.
         */
        String blacklistKey =
                jwtId.isBlank()
                        ? BLACKLIST_PREFIX + "unused"
                        : blacklistKey(jwtId);

        Long result = redisTemplate.execute(
                LOGOUT_SCRIPT,
                List.of(
                        refreshKey(userId),
                        sessionKey(userId),
                        blacklistKey
                ),
                jwtId,
                String.valueOf(ttlMillis)
        );

        validateScriptResult(result);
    }

    /**
     * Classpath의 Lua Script를 Redis Script 객체로 생성합니다.
     */
    private static DefaultRedisScript<Long> createScript(
            String path
    ) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setLocation(
                new ClassPathResource(path)
        );
        script.setResultType(Long.class);

        return script;
    }

    /**
     * Lua Script가 정상적으로 완료되었는지 확인합니다.
     */
    private void validateScriptResult(Long result) {
        if (!Long.valueOf(1L).equals(result)) {
            throw new IllegalStateException(
                    "Redis auth state script execution failed."
            );
        }
    }

    private String refreshKey(UUID userId) {
        return REFRESH_PREFIX + userId;
    }

    private String sessionKey(UUID userId) {
        return SESSION_PREFIX + userId;
    }

    private String blacklistKey(String jwtId) {
        return BLACKLIST_PREFIX + jwtId;
    }

    @Override
    public boolean rotateRefreshToken(
            UUID userId,
            String currentRefreshTokenHash,
            String newRefreshTokenHash,
            Duration ttl
    ) {
        Long result = redisTemplate.execute(
                REISSUE_SCRIPT,
                List.of(
                        refreshKey(userId),
                        sessionKey(userId)
                ),
                currentRefreshTokenHash,
                newRefreshTokenHash,
                String.valueOf(ttl.toMillis())
        );

        return Long.valueOf(1L).equals(result);
    }

}