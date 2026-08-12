package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.constant.JwtClaimConstants;
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
 * {@link JwtHeaderRelayWebFilter}의 내부 인증 Header 처리 동작을 검증합니다.
 *
 * <p>클라이언트가 전달한 내부 인증 Header를 제거하고,
 * Gateway에서 검증된 JWT Claim을 이용하여 내부 서비스용 Header를
 * 다시 생성하는지 확인합니다.</p>
 */
class JwtHeaderRelayWebFilterTest {

    private static final String MASTER_ADMIN =
            "MASTER_ADMIN";

    private static final String AUTHENTICATED_USERNAME =
            "master-user";

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
     * 인증된 JWT의 사용자 ID, 사용자명 및 역할이
     * 내부 서비스용 Header로 전달되는지 확인합니다.
     *
     * <p>MASTER_ADMIN은 허브 또는 업체에 소속되지 않으므로
     * 소속 관련 Header는 전달되지 않아야 합니다.</p>
     */
    @Test
    void authenticatedJwtRelaysUserIdAndRoleHeaders() {
        UUID userId =
                UUID.randomUUID();

        JwtAuthenticationToken authentication =
                createAuthentication(
                        userId,
                        MASTER_ADMIN
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
                        HeaderConstants.USERNAME
                )
        ).isEqualTo(
                AUTHENTICATED_USERNAME
        );

        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ROLE
                )
        ).isEqualTo(
                MASTER_ADMIN
        );

        /*
         * MASTER_ADMIN은 소속 정보가 없으므로 소속 Header가
         * 생성되지 않아야 합니다.
         */
        assertThat(
                headers.containsKey(
                        HeaderConstants.AFFILIATION_ID
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.AFFILIATION_TYPE
                )
        ).isFalse();

        verifyNoInteractions(errorResponseWriter);
    }

    /**
     * 클라이언트가 내부 인증 Header를 임의로 전달하더라도
     * 해당 값이 제거되고 JWT에서 검증된 값으로 다시 설정되는지
     * 확인합니다.
     *
     * <p>JWT에 존재하지 않는 소속 정보는 클라이언트가 전달했더라도
     * 내부 서비스로 전달되지 않아야 합니다.</p>
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
                        MASTER_ADMIN
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
         * 클라이언트가 전달한 위조 사용자 ID가 아니라
         * JWT subject의 사용자 ID로 교체되어야 합니다.
         */
        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ID
                )
        ).isEqualTo(
                authenticatedUserId.toString()
        );

        /*
         * 클라이언트가 전달한 fake-user가 아니라
         * JWT에서 검증된 username으로 교체되어야 합니다.
         */
        assertThat(
                headers.getFirst(
                        HeaderConstants.USERNAME
                )
        ).isEqualTo(
                AUTHENTICATED_USERNAME
        );

        /*
         * 클라이언트가 전달한 FAKE_ROLE이 아니라
         * JWT에서 검증된 역할로 교체되어야 합니다.
         */
        assertThat(
                headers.getFirst(
                        HeaderConstants.USER_ROLE
                )
        ).isEqualTo(
                MASTER_ADMIN
        );

        /*
         * MASTER_ADMIN JWT에는 소속 Claim이 없으므로
         * 클라이언트가 위조한 소속 Header는 제거된 상태로
         * 유지되어야 합니다.
         */
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
     * 인증되지 않은 요청에서도 클라이언트가 전달한 내부 인증
     * Header가 모두 제거되는지 확인합니다.
     *
     * <p>로그인과 회원가입 같은 공개 API 요청에는 인증 Principal이
     * 존재하지 않으므로, Header를 다시 생성하지 않고 제거된 요청을
     * 다음 Filter로 전달해야 합니다.</p>
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
                                MASTER_ADMIN
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
                        HeaderConstants.USERNAME
                )
        ).isFalse();

        assertThat(
                headers.containsKey(
                        HeaderConstants.USER_ROLE
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
     * 테스트에서 사용할 MASTER_ADMIN JWT Authentication을 생성합니다.
     *
     * <p>현재 {@link JwtHeaderRelayWebFilter}는 userId, username,
     * role Claim을 필수로 검증하므로 테스트 JWT에도 동일한 Claim을
     * 포함해야 합니다.</p>
     */
    private JwtAuthenticationToken createAuthentication(
            UUID userId,
            String role
    ) {
        Instant now =
                Instant.now();

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
                        JwtClaimConstants.USERNAME,
                        AUTHENTICATED_USERNAME
                )
                .claim(
                        JwtClaimConstants.ROLE,
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