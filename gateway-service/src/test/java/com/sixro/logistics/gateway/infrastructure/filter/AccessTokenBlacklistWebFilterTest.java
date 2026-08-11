package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.gateway.application.security.TokenBlacklistService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 로그아웃된 Access Token의 재사용을 차단하는 Gateway 블랙리스트
 * 필터의 핵심 분기를 검증하는 단위 테스트입니다.
 *
 * <p>실제 Redis와 Web Server를 사용하지 않으며, Redis 장애 시
 * 인증 요청을 허용하지 않는 Fail Closed 정책도 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistWebFilterTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private GatewayErrorResponseWriter errorResponseWriter;

    @Mock
    private WebFilterChain filterChain;

    private AccessTokenBlacklistWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AccessTokenBlacklistWebFilter(
                tokenBlacklistService,
                errorResponseWriter
        );
    }

    @Test
    @DisplayName("블랙리스트에 없는 Access Token은 요청을 통과시킨다")
    void filter_notBlacklisted_passesRequest() {
        String tokenId = UUID.randomUUID().toString();
        ServerWebExchange exchange = authenticatedExchange(tokenId);

        when(tokenBlacklistService.isBlacklisted(tokenId))
                .thenReturn(Mono.just(false));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(errorResponseWriter);
    }

    @Test
    @DisplayName("블랙리스트 Access Token은 요청을 차단한다")
    void filter_blacklisted_blocksRequest() {
        String tokenId = UUID.randomUUID().toString();
        ServerWebExchange exchange = authenticatedExchange(tokenId);

        when(tokenBlacklistService.isBlacklisted(tokenId))
                .thenReturn(Mono.just(true));
        when(errorResponseWriter.write(
                exchange,
                GatewaySecurityErrorCode.BLACKLISTED_ACCESS_TOKEN
        )).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(errorResponseWriter).write(
                exchange,
                GatewaySecurityErrorCode.BLACKLISTED_ACCESS_TOKEN
        );
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("JTI가 없는 Access Token은 유효하지 않은 토큰으로 차단한다")
    void filter_missingTokenId_blocksRequest() {
        ServerWebExchange exchange = authenticatedExchange(null);

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
        verifyNoInteractions(tokenBlacklistService);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Redis 블랙리스트 조회에 실패하면 Fail Closed 정책으로 차단한다")
    void filter_blacklistStoreFailure_failsClosed() {
        String tokenId = UUID.randomUUID().toString();
        ServerWebExchange exchange = authenticatedExchange(tokenId);

        when(tokenBlacklistService.isBlacklisted(tokenId))
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
    @DisplayName("인증 정보가 없는 공개 요청은 블랙리스트 검사 없이 통과한다")
    void filter_unauthenticatedRequest_passesRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login").build()
        );

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(
                tokenBlacklistService,
                errorResponseWriter
        );
    }

    private ServerWebExchange authenticatedExchange(String tokenId) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("test-access-token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800));

        if (tokenId != null) {
            builder.claim("jti", tokenId);
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
