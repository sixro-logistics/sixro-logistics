package com.sixro.logistics.gateway.infrastructure.security;

import com.sixro.logistics.gateway.domain.exception.GatewaySecurityErrorCode;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Gateway에서 JWT 인증에 실패한 요청의 401 응답을 처리합니다.
 *
 * <p>JWT 검증 실패 원인에 따라 다음 Gateway 오류 코드로 변환합니다.</p>
 *
 * <ul>
 *     <li>GW001: 형식, 서명, 필수 Claim 등이 유효하지 않은 Access Token</li>
 *     <li>GW002: 유효기간이 만료된 Access Token</li>
 * </ul>
 *
 * <p>JWT 인증은 성공했지만 요청 리소스에 대한 권한이 없는 경우는
 * {@link GatewayAccessDeniedHandler}가 403 응답을 처리합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class GatewayAuthenticationEntryPoint
        implements ServerAuthenticationEntryPoint {

    private final GatewayErrorResponseWriter errorResponseWriter;

    /**
     * JWT 인증 실패 원인을 판별하여 공통 오류 응답을 작성합니다.
     */
    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException exception
    ) {
        GatewaySecurityErrorCode errorCode = isExpiredToken(exception)
                ? GatewaySecurityErrorCode.EXPIRED_ACCESS_TOKEN
                : GatewaySecurityErrorCode.INVALID_ACCESS_TOKEN;

        return errorResponseWriter.write(exchange, errorCode);
    }

    /**
     * 예외 원인 체인에서 JWT 검증 오류를 찾아
     * Access Token 만료 오류가 포함되어 있는지 확인합니다.
     *
     * <p>Spring Security의 JwtValidationException이 제공하는
     * OAuth2Error description을 이용해 만료 여부를 판별합니다.</p>
     *
     * <p>현재는 error description의 "expired" 문자열에 의존하므로,
     * 향후 Gateway JWT 오류 분류가 더 세분화될 경우
     * 전용 Validator 또는 오류 매핑 구조로 개선할 수 있습니다.</p>
     *
     * @param throwable JWT 인증 과정에서 발생한 예외
     * @return 토큰 만료 오류이면 true
     */
    private boolean isExpiredToken(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof JwtValidationException jwtException) {
                return jwtException.getErrors().stream()
                        .map(error -> error.getDescription()
                                .toLowerCase(Locale.ROOT))
                        .anyMatch(description ->
                                description.contains("expired"));
            }

            current = current.getCause();
        }

        return false;
    }
}
