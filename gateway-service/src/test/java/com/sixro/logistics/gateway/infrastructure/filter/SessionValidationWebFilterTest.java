package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.JwtClaimConstants;
import com.sixro.logistics.gateway.application.security.SessionValidationService;
import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JWT의 sessionId와 Redis의 현재 로그인 Session을 비교하는
 * Gateway 필터의 핵심 분기를 검증하는 단위 테스트입니다.
 *
 * <p>실제 Redis와 Web Server를 사용하지 않으며, Redis 장애 시
 * 요청을 차단하는 Fail Closed 정책도 함께 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class SessionValidationWebFilterTest {

    @Mock
    private SessionValidationService sessionValidationService;

    @Mock
    private GatewayErrorResponseWriter errorResponseWriter;

    @Mock
    private WebFilterChain filterChain;

    private SessionValidationWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SessionValidationWebFilter(
                sessionValidationService,
                errorResponseWriter
        );
    }

    @Test
    @DisplayName("현재 로그인 Session과 일치하면 요청을 통과시킨다")
    void filter_currentSession_passesRequest() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ServerWebExchange exchange = authenticatedExchange(
                userId.toString(),
                sessionId.toString()
        );

        when(sessionValidationService.isCurrentSession(userId, sessionId))
                .thenReturn(Mono.just(true));
        when(filterChain.filter(any(ServerWebExchange.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(errorResponseWriter);
    }

    @Test
    @DisplayName("현재 로그인 Session과 일치하지 않으면 요청을 차단한다")
    void filter_mismatchedSession_blocksRequest() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ServerWebExchange exchange = authenticatedExchange(
                userId.toString(),
                sessionId.toString()
        );

        when(sessionValidationService.isCurrentSession(userId, sessionId))
                .thenReturn(Mono.just(false));
        when(errorResponseWriter.write(
                exchange,
                GatewaySecurityErrorCode.INVALID_SESSION
        )).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(errorResponseWriter).write(
                exchange,
                GatewaySecurityErrorCode.INVALID_SESSION
        );
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("sessionId Claim이 없으면 유효하지 않은 토큰으로 차단한다")
    void filter_missingSessionId_blocksRequest() {
        ServerWebExchange exchange = authenticatedExchange(
                UUID.randomUUID().toString(),
                null
        );

        when(errorResponseWriter.write(
                exchange,
                GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
        )).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(errorResponseWriter).write(
                exchange,
                GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
        );
        verifyNoInteractions(sessionValidationService);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("사용자 ID 또는 sessionId가 UUID 형식이 아니면 요청을 차단한다")
    void filter_invalidUuidClaim_blocksRequest() {
        ServerWebExchange exchange = authenticatedExchange(
                "invalid-user-id",
                "invalid-session-id"
        );

        when(errorResponseWriter.write(
                exchange,
                GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
        )).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(errorResponseWriter).write(
                exchange,
                GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
        );
        verifyNoInteractions(sessionValidationService);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Redis Session 조회에 실패하면 Fail Closed 정책으로 차단한다")
    void filter_sessionStoreFailure_failsClosed() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ServerWebExchange exchange = authenticatedExchange(
                userId.toString(),
                sessionId.toString()
        );

        when(sessionValidationService.isCurrentSession(userId, sessionId))
                .thenReturn(Mono.error(
                        new RuntimeException("Redis connection failed")
                ));
        when(errorResponseWriter.write(
                exchange,
                GatewaySecurityErrorCode.AUTH_STATE_UNAVAILABLE
        )).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(errorResponseWriter).write(
                exchange,
                GatewaySecurityErrorCode.AUTH_STATE_UNAVAILABLE
        );
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("인증 정보가 없는 공개 요청은 Session 검증 없이 통과한다")
    void filter_unauthenticatedRequest_passesRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login").build()
        );

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(
                sessionValidationService,
                errorResponseWriter
        );
    }

    private ServerWebExchange authenticatedExchange(
            String userId,
            String sessionId
    ) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("test-access-token")
                .header("alg", "RS256")
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800));

        if (sessionId != null) {
            builder.claim(JwtClaimConstants.SESSION_ID, sessionId);
        }

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(builder.build());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me").build()
        );

        SecurityContext securityContext =
                new SecurityContextImpl(authentication);

        return new SecurityContextServerWebExchange(
                exchange,
                Mono.just(securityContext)
        );
    }
}
