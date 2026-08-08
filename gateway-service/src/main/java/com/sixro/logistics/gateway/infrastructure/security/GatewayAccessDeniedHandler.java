package com.sixro.logistics.gateway.infrastructure.security;

import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * JWT 인증은 완료되었지만
 * 요청한 리소스에 대한 권한이 없는 경우(403)를 처리합니다.
 *
 * <p>인증 실패(401)는 GatewayAuthenticationEntryPoint가 처리합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class GatewayAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final GatewayErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            AccessDeniedException exception
    ) {
        return errorResponseWriter.write(
                exchange,
                GatewaySecurityErrorCode.ACCESS_DENIED
        );
    }
}