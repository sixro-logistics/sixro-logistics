package com.sixro.logistics.gateway.infrastructure.security;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.test.config.RedisTestContainerConfig;
import com.sixro.logistics.gateway.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void gatewayErrorResponseUsesSameRequestIdInHeaderAndBody()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        String token = TestJwtFactory.createExpiredToken(
                tokenId,
                userId,
                sessionId
        );

        EntityExchangeResult<byte[]> result =
                webTestClient.get()
                        .uri("/api/v1/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .exchange()
                        .expectStatus()
                        .isUnauthorized()
                        .expectHeader()
                        .exists(HeaderConstants.REQUEST_ID)
                        .expectBody()
                        .returnResult();

        String requestId =
                result.getResponseHeaders()
                        .getFirst(HeaderConstants.REQUEST_ID);

        assertThat(requestId)
                .isNotBlank();

        try {
            JsonNode responseBody =
                    objectMapper.readTree(
                            result.getResponseBody()
                    );

            assertThat(
                    responseBody
                            .get("requestId")
                            .asText()
            ).isEqualTo(requestId);

        } catch (Exception exception) {
            throw new AssertionError(
                    "Gateway 오류 응답 JSON 파싱에 실패했습니다.",
                    exception
            );
        }
    }

    @Test
    void nonMasterAdminCannotAccessMasterAdminUserApi()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        String token = TestJwtFactory.createValidToken(
                tokenId,
                userId,
                sessionId,
                "HUB_ADMIN"
        );

        saveSession(userId, sessionId);

        try {
            webTestClient.get()
                    .uri("/api/v1/users/" + targetUserId)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    )
                    .exchange()
                    .expectStatus()
                    .isForbidden()
                    .expectBody()
                    .jsonPath("$.code")
                    .isEqualTo("GW004")
                    .jsonPath("$.message")
                    .isEqualTo("접근 권한이 없습니다.");

        } finally {
            redisTemplate.delete(
                    SESSION_PREFIX + userId
            ).block();
        }
    }

    @Test
    void masterAdminPassesGatewayRoleAuthorization()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        String token = TestJwtFactory.createValidToken(
                tokenId,
                userId,
                sessionId,
                "MASTER_ADMIN"
        );

        saveSession(userId, sessionId);

        try {
            webTestClient.get()
                    .uri("/api/v1/users/" + targetUserId)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    )
                    .exchange()
                    .expectStatus()
                    .value(status ->
                            assertThat(status)
                                    .isNotEqualTo(403)
                    );

        } finally {
            redisTemplate.delete(
                    SESSION_PREFIX + userId
            ).block();
        }
    }
}