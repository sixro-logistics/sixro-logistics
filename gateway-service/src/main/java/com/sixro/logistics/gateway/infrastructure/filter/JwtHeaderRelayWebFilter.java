package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * JWT 인증이 완료된 요청의 Claim을 내부 서비스용 Header로 변환합니다.
 *
 * <p>클라이언트가 전달한 내부 인증 Header를 제거한 뒤,
 * Gateway에서 검증된 JWT Claim을 기반으로 Header를 다시 설정합니다.</p>
 *
 * <p>내부 서비스는 외부에서 직접 접근할 수 없도록 네트워크 수준에서도
 * Gateway 경유만 허용해야 합니다.</p>
 */
@RequiredArgsConstructor
public class JwtHeaderRelayWebFilter implements WebFilter {

    private static final String ROLE_CLAIM = "role";

    private final GatewayErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        ServerWebExchange sanitizedExchange =
                removeInternalHeaders(exchange);

        /*
         * Optional 변환은 Mono<Void> 뒤에 switchIfEmpty를 적용했을 때
         * 필터 체인이 중복 실행될 수 있는 문제를 방지합니다.
         */
        return sanitizedExchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(authentication -> authentication
                        .map(jwtAuthentication ->
                                relayHeaders(
                                        sanitizedExchange,
                                        chain,
                                        jwtAuthentication
                                )
                        )
                        .orElseGet(() ->
                                chain.filter(sanitizedExchange)
                        )
                );
    }

    /**
     * 검증된 JWT Claim을 내부 서비스에서 사용하는 Header로 변환합니다.
     */
    private Mono<Void> relayHeaders(
            ServerWebExchange exchange,
            WebFilterChain chain,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getToken().getSubject();
        String role = authentication.getToken()
                .getClaimAsString(ROLE_CLAIM);

        if (!hasRequiredClaims(userId, role)
                || !isValidUuid(userId)) {
            return errorResponseWriter.write(
                    exchange,
                    GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.set(
                            HeaderConstants.USER_ID,
                            userId
                    );
                    headers.set(
                            HeaderConstants.USER_ROLE,
                            role
                    );
                }))
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * 프로젝트에서 필수로 사용하는 JWT Claim이 존재하는지 확인합니다.
     */
    private boolean hasRequiredClaims(
            String userId,
            String role
    ) {
        return userId != null
                && !userId.isBlank()
                && role != null
                && !role.isBlank();
    }

    /**
     * 사용자 식별자가 UUID 형식인지 확인합니다.
     */
    private boolean isValidUuid(String userId) {
        try {
            UUID.fromString(userId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 클라이언트가 전달한 내부 인증용 Header를 제거합니다.
     *
     * <p>외부 사용자가 내부 Header를 임의로 설정하는 Header Spoofing을
     * 방지하기 위해 JWT Claim을 전달하기 전에 모두 제거합니다.</p>
     */
    private ServerWebExchange removeInternalHeaders(
            ServerWebExchange exchange
    ) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(HeaderConstants.USER_ID);
                    headers.remove(HeaderConstants.USER_ROLE);
                    headers.remove(HeaderConstants.USERNAME);
                    headers.remove(HeaderConstants.AFFILIATION_TYPE);
                    headers.remove(HeaderConstants.AFFILIATION_ID);
                }))
                .build();
    }
}