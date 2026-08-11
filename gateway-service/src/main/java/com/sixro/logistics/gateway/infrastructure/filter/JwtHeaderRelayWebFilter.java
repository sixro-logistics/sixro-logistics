package com.sixro.logistics.gateway.infrastructure.filter;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.constant.JwtClaimConstants;
import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
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

    private static final String MASTER_ADMIN = "MASTER_ADMIN";
    private static final String HUB_ADMIN = "HUB_ADMIN";
    private static final String DELIVERY_MANAGER = "DELIVERY_MANAGER";
    private static final String COMPANY_MANAGER = "COMPANY_MANAGER";

    private static final String HUB = "HUB";
    private static final String COMPANY = "COMPANY";

    private final GatewayErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        /*
         * 클라이언트가 직접 전달한 내부 인증 Header를 제거합니다.
         * 인증되지 않은 요청에서도 해당 Header가 내부 서비스로
         * 전달되지 않도록 가장 먼저 처리합니다.
         */
        ServerWebExchange sanitizedExchange = removeInternalHeaders(exchange);

        /*
         * 인증이 완료된 요청은 Principal이
         * JwtAuthenticationToken 타입으로 전달됩니다.
         *
         * 인증이 필요 없는 공개 API 요청은 Principal이 없으므로
         * Header가 제거된 요청을 그대로 다음 Filter로 전달합니다.
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
        Jwt jwt = authentication.getToken();

        String userId = jwt.getSubject();

        String username = jwt.getClaimAsString(JwtClaimConstants.USERNAME);
        String role = jwt.getClaimAsString(JwtClaimConstants.ROLE);
        String affiliationId = jwt.getClaimAsString(JwtClaimConstants.AFFILIATION_ID);
        String affiliationType = jwt.getClaimAsString(JwtClaimConstants.AFFILIATION_TYPE);

        if (!hasValidClaims(
                userId,
                username,
                role,
                affiliationId,
                affiliationType
        )) {
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
                                    HeaderConstants.USERNAME,
                                    username
                            );
                            headers.set(
                                    HeaderConstants.USER_ROLE,
                                    role
                            );
                            /*
                             * 마스터 관리자는 소속 정보가 없습니다.
                             * 나머지 역할은 검증을 통과한 소속 정보를
                             * Header로 전달합니다.
                             */

                            if (affiliationId != null) {
                                headers.set(
                                        HeaderConstants
                                                .AFFILIATION_ID,
                                        affiliationId
                                );
                            }

                            if (affiliationType != null) {
                                headers.set(
                                        HeaderConstants
                                                .AFFILIATION_TYPE,
                                        affiliationType
                                );
                            }
                        })
                )
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * JWT에 필요한 사용자 식별 정보와 역할별 소속 정보를 검증합니다.
     */
    private boolean hasValidClaims(
            String userId,
            String username,
            String role,
            String affiliationId,
            String affiliationType
    ) {
        if (isBlank(userId)
                || !isValidUuid(userId)
                || isBlank(username)
                || isBlank(role)) {
            return false;
        }

        return switch (role) {
            /*
             * 마스터 관리자는 특정 허브 또는 업체에
             * 소속되지 않으므로 소속 Claim이 없어야 합니다.
             */
            case MASTER_ADMIN -> affiliationId == null && affiliationType == null;

            /*
             * 허브 관리자와 배송 담당자는 허브에 소속됩니다.
             */
            case HUB_ADMIN, DELIVERY_MANAGER -> isValidUuid(affiliationId) && HUB.equals(affiliationType);

            /*
             * 업체 담당자는 업체에 소속됩니다.
             */
            case COMPANY_MANAGER -> isValidUuid(affiliationId) && COMPANY.equals(affiliationType);

            /*
             * 시스템에서 정의하지 않은 역할은 허용하지 않습니다.
             */
            default -> false;
        };
    }

    /**
     * 문자열이 null 또는 공백인지 확인합니다.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 사용자 식별자가 UUID 형식인지 확인합니다.
     */
    private boolean isValidUuid(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            UUID.fromString(value);
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
                .request(request ->
                        request.headers(headers -> {
                            headers.remove(
                                    HeaderConstants.USER_ID
                            );
                            headers.remove(
                                    HeaderConstants.USERNAME
                            );
                            headers.remove(
                                    HeaderConstants.USER_ROLE
                            );
                            headers.remove(
                                    HeaderConstants.AFFILIATION_ID
                            );
                            headers.remove(
                                    HeaderConstants
                                            .AFFILIATION_TYPE
                            );
                        })
                )
                .build();
    }
}