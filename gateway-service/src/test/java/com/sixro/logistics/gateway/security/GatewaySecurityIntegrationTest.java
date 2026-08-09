package com.sixro.logistics.gateway.security;

import com.sixro.logistics.common.test.config.RedisTestContainerConfig;
import com.sixro.logistics.gateway.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

@ImportTestcontainers(RedisTestContainerConfig.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class GatewaySecurityIntegrationTest {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String SESSION_PREFIX = "session:";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Test
    void expiredAccessTokenReturnsGW002() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        String token = TestJwtFactory.createExpiredToken(
                tokenId,
                userId,
                sessionId
        );

        webTestClient.get()
                .uri("/api/v1/users/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("GW002")
                .jsonPath("$.message")
                .isEqualTo("만료된 Access Token입니다.");
    }

    @Test
    void blacklistedAccessTokenReturnsGW003()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        String token = TestJwtFactory.createValidToken(
                tokenId,
                userId,
                sessionId
        );

        /*
         * Session 검증까지 도달할 수 있도록
         * 현재 로그인 Session을 Redis에 먼저 저장합니다.
         */
        saveSession(
                userId,
                sessionId
        );

        /*
         * Auth Service와 동일한 blacklist Key 규칙을 사용합니다.
         */
        StepVerifier.create(
                        redisTemplate.opsForValue().set(
                                BLACKLIST_PREFIX + tokenId,
                                "logout",
                                Duration.ofMinutes(30)
                        )
                )
                .expectNext(true)
                .verifyComplete();

        try {
            webTestClient.get()
                    .uri("/api/v1/users/me")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    )
                    .exchange()
                    .expectStatus()
                    .isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code")
                    .isEqualTo("GW003")
                    .jsonPath("$.message")
                    .isEqualTo(
                            "이미 로그아웃된 토큰입니다."
                    );
        } finally {
            redisTemplate.delete(
                    BLACKLIST_PREFIX + tokenId
            ).block();

            redisTemplate.delete(
                    SESSION_PREFIX + userId
            ).block();
        }
    }

    @Test
    void mismatchedSessionReturnsGW006()
            throws Exception {

        UUID userId = UUID.randomUUID();

        UUID tokenSessionId =
                UUID.randomUUID();

        UUID currentSessionId =
                UUID.randomUUID();

        String tokenId =
                UUID.randomUUID().toString();

        String token = TestJwtFactory.createValidToken(
                tokenId,
                userId,
                tokenSessionId
        );

        /*
         * JWT의 sessionId와 다른 현재 Session을 저장합니다.
         *
         * 새 로그인으로 Session이 교체된 상황을 재현합니다.
         */
        saveSession(
                userId,
                currentSessionId
        );

        try {
            webTestClient.get()
                    .uri("/api/v1/users/me")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    )
                    .exchange()
                    .expectStatus()
                    .isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code")
                    .isEqualTo("GW006")
                    .jsonPath("$.message")
                    .isEqualTo(
                            "현재 유효한 로그인 세션이 아닙니다."
                    );
        } finally {
            redisTemplate.delete(
                    SESSION_PREFIX + userId
            ).block();
        }
    }

    /**
     * Auth Service의 로그인 상태 저장 규칙과 동일하게
     * 테스트용 Session을 Redis에 저장합니다.
     */
    private void saveSession(
            UUID userId,
            UUID sessionId
    ) {
        StepVerifier.create(
                        redisTemplate.opsForValue().set(
                                SESSION_PREFIX + userId,
                                sessionId.toString(),
                                Duration.ofMinutes(30)
                        )
                )
                .expectNext(true)
                .verifyComplete();
    }
}