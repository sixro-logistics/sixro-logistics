package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.JwtClaimConstants;
import com.sixro.logistics.gateway.application.security.SessionValidationService;
import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * JWT 인증이 완료된 Access Token의 sessionId가
 * Redis에 저장된 현재 로그인 Session과 일치하는지 검증하는 필터입니다.
 *
 * <p>Auth Service는 사용자별 현재 로그인 Session을
 * {@code session:{userId}} 형식으로 Redis에 저장합니다.</p>
 *
 * <p>Gateway는 JWT의 {@code sessionId}와 Redis의 현재 Session을 비교하여
 * 새 로그인으로 교체된 이전 Access Token의 사용을 차단합니다.</p>
 *
 * <p>Redis 장애 등으로 인증 상태를 확인할 수 없는 경우에는
 * Fail Closed 정책에 따라 요청을 차단합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SessionValidationWebFilter implements WebFilter {

    private final SessionValidationService sessionValidationService;
    private final GatewayErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .flatMap(this::validateSession)

                /*
                 * 회원가입, 로그인, 토큰 재발급 등의 공개 API처럼
                 * 인증 객체가 없는 요청은 Session 검증 없이 통과합니다.
                 */
                .defaultIfEmpty(
                        SessionCheckResult.pass()
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
     * JWT에서 사용자 ID와 sessionId를 추출하여
     * 현재 Redis Session과 일치하는지 확인합니다.
     */
    private Mono<SessionCheckResult> validateSession(
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getToken().getSubject();

        String sessionId = authentication.getToken().getClaimAsString(JwtClaimConstants.SESSION_ID);

        /*
         * userId 또는 sessionId가 누락되었거나
         * UUID 형식이 올바르지 않은 경우
         * 유효하지 않은 Access Token으로 처리합니다.
         */
        if (!hasRequiredClaims(userId, sessionId)) {
            return Mono.just(
                    SessionCheckResult.invalidTokenResult()
            );
        }

        UUID parsedUserId;
        UUID parsedSessionId;

        try {
            parsedUserId = UUID.fromString(userId);
            parsedSessionId = UUID.fromString(sessionId);
        } catch (IllegalArgumentException exception) {
            return Mono.just(
                    SessionCheckResult.invalidTokenResult()
            );
        }

        return sessionValidationService
                .isCurrentSession(
                        parsedUserId,
                        parsedSessionId
                )
                .map(SessionCheckResult::from)
                .onErrorResume(exception -> {
                    log.error(
                            "현재 로그인 Session 조회 실패. userId={}, sessionId={}",
                            userId,
                            sessionId,
                            exception
                    );

                    /*
                     * Redis 장애 등으로 현재 인증 상태를 확인하지 못하면
                     * Fail Closed 정책에 따라 요청을 허용하지 않습니다.
                     */
                    return Mono.just(
                            SessionCheckResult.unavailableResult()
                    );
                });
    }

    /**
     * Session 검증 결과에 따라 요청 진행 여부를 결정합니다.
     */
    private Mono<Void> handleResult(
            ServerWebExchange exchange,
            WebFilterChain chain,
            SessionCheckResult result
    ) {
        if (result.invalidToken()) {
            return errorResponseWriter.write(
                    exchange,
                    GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        if (result.invalidSession()) {
            return errorResponseWriter.write(
                    exchange,
                    GatewaySecurityErrorCode.INVALID_SESSION
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
     * Session 검증에 필요한 JWT Claim이 존재하는지 확인합니다.
     */
    private boolean hasRequiredClaims(
            String userId,
            String sessionId
    ) {
        return userId != null
                && !userId.isBlank()
                && sessionId != null
                && !sessionId.isBlank();
    }

    /**
     * Access Token Session 검증 결과입니다.
     *
     * @param invalidToken   필수 Claim이 없거나 형식이 잘못된 토큰인지 여부
     * @param invalidSession 현재 로그인 Session과 일치하지 않는지 여부
     * @param unavailable    Redis 인증 상태 조회 실패 여부
     */
    private record SessionCheckResult(
            boolean invalidToken,
            boolean invalidSession,
            boolean unavailable
    ) {

        private static SessionCheckResult pass() {
            return new SessionCheckResult(
                    false,
                    false,
                    false
            );
        }

        private static SessionCheckResult invalidTokenResult() {
            return new SessionCheckResult(
                    true,
                    false,
                    false
            );
        }

        private static SessionCheckResult from(
                boolean currentSession
        ) {
            return new SessionCheckResult(
                    false,
                    !currentSession,
                    false
            );
        }

        private static SessionCheckResult unavailableResult() {
            return new SessionCheckResult(
                    false,
                    false,
                    true
            );
        }
    }
}