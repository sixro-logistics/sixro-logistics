package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.gateway.application.security.TokenBlacklistService;
import com.sixro.logistics.gateway.domain.exception.AuthErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT 인증이 완료된 Access Token이
 * Redis 블랙리스트에 등록되어 있는지 검사하는 필터입니다.
 *
 * <p>Spring Security의 JWT 인증 이후 실행되며,
 * 로그아웃된 Access Token은 더 이상 사용할 수 없도록 차단합니다.</p>
 */
@RequiredArgsConstructor
public class AccessTokenBlacklistWebFilter implements WebFilter {

    private final TokenBlacklistService tokenBlacklistService;
    private final GatewayErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .flatMap(jwtAuthentication -> {
                    // JWT의 JTI(Token ID)를 조회합니다.
                    String tokenId = jwtAuthentication.getToken().getId();

                    /*
                     * JWT는 정상적으로 인증되었지만
                     * JTI가 존재하지 않으면 프로젝트에서 사용하는
                     * 올바른 Access Token이 아니므로 인증 실패로 처리합니다.
                     */
                    if (tokenId == null || tokenId.isBlank()) {
                        return Mono.just(BlacklistCheckResult.invalid());
                    }

                    /*
                     * Redis에서 Access Token(JTI)이
                     * 블랙리스트에 등록되어 있는지 확인합니다.
                     */
                    return tokenBlacklistService.isBlacklisted(tokenId)
                            .map(BlacklistCheckResult::from);
                })

                /*
                 * 공개 API 등 인증 객체가 없는 요청은
                 * 블랙리스트 검사 없이 그대로 통과합니다.
                 */
                .defaultIfEmpty(BlacklistCheckResult.pass())
                .flatMap(result -> handleResult(
                        exchange,
                        chain,
                        result
                ));
    }

    /**
     * 블랙리스트 검사 결과에 따라 요청을 계속 진행하거나
     * 인증 오류 응답을 반환합니다.
     */
    private Mono<Void> handleResult(
            ServerWebExchange exchange,
            WebFilterChain chain,
            BlacklistCheckResult result
    ) {
        if (result.invalidToken()) {
            return errorResponseWriter.write(
                    exchange,
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        if (result.blacklisted()) {
            return errorResponseWriter.write(
                    exchange,
                    AuthErrorCode.BLACKLISTED_ACCESS_TOKEN
            );
        }

        return chain.filter(exchange);
    }

    /**
     * Access Token 블랙리스트 검사 결과입니다.
     *
     * @param invalidToken JWT는 인증되었지만
     *                     프로젝트에서 요구하는 JTI가 없는 경우
     * @param blacklisted Redis 블랙리스트 등록 여부
     */
    private record BlacklistCheckResult(
            boolean invalidToken,
            boolean blacklisted
    ) {
        /**
         * 블랙리스트 검사 통과
         */
        private static BlacklistCheckResult pass() {
            return new BlacklistCheckResult(false, false);
        }

        /**
         * 올바르지 않은 Access Token
         */
        private static BlacklistCheckResult invalid() {
            return new BlacklistCheckResult(true, false);
        }

        /**
         * Redis 블랙리스트 조회 결과
         */
        private static BlacklistCheckResult from(boolean blacklisted) {
            return new BlacklistCheckResult(false, blacklisted);
        }
    }
}