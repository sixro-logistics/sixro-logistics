package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.gateway.application.security.TokenBlacklistService;
import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT 인증이 완료된 Access Token이
 * Redis 블랙리스트에 등록되어 있는지 검사하는 필터입니다.
 *
 * <p>로그아웃된 Access Token은 더 이상 사용할 수 없도록 차단합니다.</p>
 *
 * <p>Redis 조회에 실패하여 인증 상태를 확인할 수 없는 경우에는
 * Fail Closed 정책에 따라 요청을 차단하고
 * 503 Service Unavailable 응답을 반환합니다.</p>
 */
@Slf4j
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
                .flatMap(this::checkBlacklist)
                /*
                 * 공개 API처럼 인증 객체가 없는 요청은
                 * 블랙리스트 검사 없이 통과합니다.
                 */
                .defaultIfEmpty(
                        BlacklistCheckResult.pass()
                )
                .flatMap(result ->
                        handleResult(
                                exchange,
                                chain,
                                result
                        )
                );
    }

    /**
     * 인증된 JWT의 JTI를 사용해 블랙리스트 상태를 확인합니다.
     */
    private Mono<BlacklistCheckResult> checkBlacklist(
            JwtAuthenticationToken authentication
    ) {
        String tokenId =
                authentication.getToken().getId();

        /*
         * Access Token blacklist 검증을 위해
         * JTI는 필수 Claim으로 취급합니다.
         */
        if (tokenId == null || tokenId.isBlank()) {
            return Mono.just(
                    BlacklistCheckResult.invalid()
            );
        }

        return tokenBlacklistService
                .isBlacklisted(tokenId)
                .map(BlacklistCheckResult::from)
                .onErrorResume(exception -> {
                    log.error(
                            "Access Token 블랙리스트 조회 실패: tokenId={}",
                            tokenId,
                            exception
                    );

                    /*
                     * Redis 장애 등으로 인증 상태를 확인하지 못한 경우
                     * Fail Closed 정책에 따라 요청을 허용하지 않습니다.
                     */
                    return Mono.just(
                            BlacklistCheckResult.unavailableResult()
                    );
                });
    }

    /**
     * 블랙리스트 검사 결과에 따라 요청 진행 여부를 결정합니다.
     */
    private Mono<Void> handleResult(
            ServerWebExchange exchange,
            WebFilterChain chain,
            BlacklistCheckResult result
    ) {
        if (result.invalidToken()) {
            return errorResponseWriter.write(
                    exchange,
                    GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        if (result.blacklisted()) {
            return errorResponseWriter.write(
                    exchange,
                    GatewaySecurityErrorCode.BLACKLISTED_ACCESS_TOKEN
            );
        }

        if (result.unavailable()) {
            return errorResponseWriter.write(
                    exchange,
                    GatewaySecurityErrorCode.AUTH_STATE_UNAVAILABLE
            );
        }

        return chain.filter(exchange);
    }

    /**
     * Access Token 블랙리스트 검사 결과입니다.
     *
     * @param invalidToken JWT에 필수 JTI가 없는지 여부
     * @param blacklisted 블랙리스트 등록 여부
     * @param unavailable 블랙리스트 저장소 조회 실패 여부
     */
    private record BlacklistCheckResult(
            boolean invalidToken,
            boolean blacklisted,
            boolean unavailable
    ) {

        private static BlacklistCheckResult pass() {
            return new BlacklistCheckResult(
                    false,
                    false,
                    false
            );
        }

        private static BlacklistCheckResult invalid() {
            return new BlacklistCheckResult(
                    true,
                    false,
                    false
            );
        }

        private static BlacklistCheckResult from(
                boolean blacklisted
        ) {
            return new BlacklistCheckResult(
                    false,
                    blacklisted,
                    false
            );
        }

        private static BlacklistCheckResult unavailableResult() {
            return new BlacklistCheckResult(
                    false,
                    false,
                    true
            );
        }
    }
}