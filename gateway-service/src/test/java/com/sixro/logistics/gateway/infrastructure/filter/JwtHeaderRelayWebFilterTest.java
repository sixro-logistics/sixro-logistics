package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.context.SecurityContextServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * JwtHeaderRelayWebFilter의 내부 인증 Header 처리 동작을 검증합니다.
 *
 * <p>클라이언트가 전달한 내부 인증 Header를 제거하고,
 * Gateway에서 검증된 JWT Claim을 기준으로
 * 내부 서비스용 Header를 다시 생성하는지 확인합니다.</p>
 */
class JwtHeaderRelayWebFilterTest {

    private GatewayErrorResponseWriter errorResponseWriter;
    private JwtHeaderRelayWebFilter filter;

    @BeforeEach
    void setUp() {
        errorResponseWriter =
                mock(GatewayErrorResponseWriter.class);

        filter = new JwtHeaderRelayWebFilter(
                errorResponseWriter
        );
    }

    /**
     * 인증된 JWT의 userId와 role이
     * 내부 서비스용 Header로 전달되는지 확인합니다.
     */
    @Test
    void authenticatedJwtRelaysUserIdAndRoleHeaders() {

        UUID userId = UUID.randomUUID();

        JwtAuthenticationToken authentication =
                createAuthentication(
                        userId,
                        "MASTER_ADMIN"
                );

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .build();

        ServerWebExchange exchange =
                createAuthenticatedExchange(
                        request,
                        authentication
                );

        AtomicReference<ServerWebExchange> capturedExchange =
                new AtomicReference<>();

        WebFilterChain chain = currentExchange -> {
            capturedExchange.set(currentExchange);
            return Mono.empty();
        };

        StepVerifier.create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        ServerWebExchange relayedExchange =
                capturedExchange.get();

        assertThat(relayedExchange)
                .isNotNull();

        HttpHeaders headers =
                relayedExchange
                        .getRequest()
                        .getHeaders();

        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ID
                )
        ).isEqualTo(
                userId.toString()
        );

        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ROLE
                )
        ).isEqualTo(
                "MASTER_ADMIN"
        );

        verifyNoInteractions(errorResponseWriter);
    }

    /**
     * 클라이언트가 임의로 전달한 내부 인증 Header를 제거하고,
     * JWT에서 검증된 userId와 role로 다시 설정하는지 확인합니다.
     */
    @Test
    void spoofedInternalHeadersAreRemovedAndReplaced() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        UUID spoofedUserId =
                UUID.randomUUID();

        UUID spoofedAffiliationId =
                UUID.randomUUID();

        JwtAuthenticationToken authentication =
                createAuthentication(
                        authenticatedUserId,
                        "MASTER_ADMIN"
                );

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .header(
                                HeaderConstants.USER_ID,
                                spoofedUserId.toString()
                        )
                        .header(
                                HeaderConstants.USER_ROLE,
                                "FAKE_ROLE"
                        )
                        .header(
                                HeaderConstants.USERNAME,
                                "fake-user"
                        )
                        .header(
                                HeaderConstants.AFFILIATION_TYPE,
                                "COMPANY"
                        )
                        .header(
                                HeaderConstants.AFFILIATION_ID,
                                spoofedAffiliationId.toString()
                        )
                        .build();

        ServerWebExchange exchange =
                createAuthenticatedExchange(
                        request,
                        authentication
                );

        AtomicReference<ServerWebExchange> capturedExchange =
                new AtomicReference<>();

        WebFilterChain chain = currentExchange -> {
            capturedExchange.set(currentExchange);
            return Mono.empty();
        };

        StepVerifier.create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        ServerWebExchange relayedExchange =
                capturedExchange.get();

        assertThat(relayedExchange)
                .isNotNull();

        HttpHeaders headers =
                relayedExchange
                        .getRequest()
                        .getHeaders();

        /*
         * 클라이언트가 전달한 userId / role이 아니라
         * JWT에서 검증된 값으로 교체되어야 합니다.
         */
        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ID
                )
        ).isEqualTo(
                authenticatedUserId.toString()
        );

        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ROLE
                )
        ).isEqualTo(
                "MASTER_ADMIN"
        );

        /*
         * Gateway가 JWT에서 생성하지 않는 내부 인증 Header는
         * downstream으로 전달되지 않아야 합니다.
         */
        assertThat(
                headers.containsKey(
                        HeaderConstants.USERNAME
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.AFFILIATION_TYPE
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.AFFILIATION_ID
                )
        ).isFalse();

        verifyNoInteractions(errorResponseWriter);
    }

    /**
     * 인증되지 않은 요청에서도 클라이언트가 전달한
     * 내부 인증 Header를 제거하는지 확인합니다.
     */
    @Test
    void unauthenticatedRequestRemovesInternalHeaders() {

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .post("/api/v1/auth/login")
                        .header(
                                HeaderConstants.USER_ID,
                                UUID.randomUUID().toString()
                        )
                        .header(
                                HeaderConstants.USER_ROLE,
                                "MASTER_ADMIN"
                        )
                        .header(
                                HeaderConstants.USERNAME,
                                "spoofed-user"
                        )
                        .header(
                                HeaderConstants.AFFILIATION_TYPE,
                                "HUB"
                        )
                        .header(
                                HeaderConstants.AFFILIATION_ID,
                                UUID.randomUUID().toString()
                        )
                        .build();

        ServerWebExchange exchange =
                MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange =
                new AtomicReference<>();

        WebFilterChain chain = currentExchange -> {
            capturedExchange.set(currentExchange);
            return Mono.empty();
        };

        StepVerifier.create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        ServerWebExchange sanitizedExchange =
                capturedExchange.get();

        assertThat(sanitizedExchange)
                .isNotNull();

        HttpHeaders headers =
                sanitizedExchange
                        .getRequest()
                        .getHeaders();

        assertThat(
                headers.containsKey(
                        HeaderConstants.USER_ID
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.USER_ROLE
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.USERNAME
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.AFFILIATION_TYPE
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.AFFILIATION_ID
                )
        ).isFalse();

        verifyNoInteractions(errorResponseWriter);
    }

    /**
     * 테스트용 JWT Authentication을 생성합니다.
     */
    private JwtAuthenticationToken createAuthentication(
            UUID userId,
            String role
    ) {

        Instant now = Instant.now();

        Jwt jwt = Jwt.withTokenValue(
                        "test-access-token"
                )
                .header(
                        "alg",
                        "RS256"
                )
                .subject(
                        userId.toString()
                )
                .claim(
                        "role",
                        role
                )
                .issuedAt(now)
                .expiresAt(
                        now.plusSeconds(1800)
                )
                .build();

        return new JwtAuthenticationToken(jwt);
    }

    /**
     * JWT Authentication이 Principal로 조회될 수 있도록
     * SecurityContext를 포함하는 ServerWebExchange를 생성합니다.
     */
    private ServerWebExchange createAuthenticatedExchange(
            MockServerHttpRequest request,
            JwtAuthenticationToken authentication
    ) {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        SecurityContext securityContext =
                new SecurityContextImpl(
                        authentication
                );

        return new SecurityContextServerWebExchange(
                exchange,
                Mono.just(securityContext)
        );
    }
}