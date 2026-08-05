package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.gateway.domain.exception.AuthErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT 인증이 완료된 요청의 Claim을 내부 서비스에서 사용할 Header로 변환합니다.
 *
 * <p>Gateway만 JWT를 검증하고,
 * 내부 서비스(User, Hub, Company 등)는 Gateway가 전달한 Header를 신뢰합니다.</p>
 *
 */
@RequiredArgsConstructor
public class JwtHeaderRelayWebFilter implements WebFilter {
    /**
     * TODO 최종 인프라 택1 적용방법 검토
     * 내부 서비스는 외부 네트워크에서 직접 접근 불가
     * 방화벽/보안 그룹/Kubernetes NetworkPolicy로 Gateway 경유만 허용
     * 내부 서비스에서도 Gateway 호출 여부를 추가 검증
    */
    /**
     * Access Token에 저장된 사용자 권한 Claim 이름
     */
    // TODO AUTH, USER SERVICE 개발 후 재점검
    private static final String ROLE_CLAIM = "role";

    private final GatewayErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        /*
         * 먼저 외부에서 전달한 내부 전용 Header를 제거합니다.
         *
         * JWT 인증이 완료된 경우에는
         * Claim을 내부 Header로 변환합니다.
         *
         * 공개 API처럼 인증 객체가 없는 요청은
         * Header만 제거한 뒤 그대로 전달합니다.
         */
        ServerWebExchange sanitizedExchange =
                removeInternalHeaders(exchange);

        return sanitizedExchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .flatMap(authentication ->
                        relayHeaders(
                                sanitizedExchange,
                                chain,
                                authentication
                        )
                )
                .switchIfEmpty(
                        Mono.defer(() ->
                                chain.filter(sanitizedExchange)
                        )
                );
    }

    /**
     * JWT Claim을 내부 서비스에서 사용하는 Header로 변환합니다.
     */
    private Mono<Void> relayHeaders(
            ServerWebExchange exchange,
            WebFilterChain chain,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getToken().getSubject();
        String role = authentication.getToken()
                .getClaimAsString(ROLE_CLAIM);

        /*
         * JWT 서명은 유효하지만 프로젝트에서 필수로 사용하는 Claim이
         * 존재하지 않는 경우 잘못된 Access Token으로 처리합니다.
         */
        if (userId == null || userId.isBlank()
                || role == null || role.isBlank()) {

            return errorResponseWriter.write(
                    exchange,
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        /*
         * 검증이 완료된 Claim만 내부 Header로 생성합니다.
         * 이후 User / Hub / Company Service는 JWT를 다시 검증하지 않고
         * Gateway가 전달한 Header를 사용합니다.
         */
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.set(HeaderConstants.USER_ID, userId);
                    headers.set(HeaderConstants.USER_ROLE, role);
                }))
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * 외부에서 전달한 내부 전용 Header를 제거합니다.
     *
     * <p>클라이언트가 X-User-Id 등을 임의로 조작하여
     * 내부 서비스를 속이는 Header Spoofing 공격을 방지합니다.</p>
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