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
 * <p>Access Token 만료 여부에 따라 다음 오류 코드로 구분합니다.</p>
 *
 * <ul>
 *     <li>A008: 형식, 서명, 필수 값 등이 유효하지 않은 Access Token</li>
 *     <li>A009: 유효기간이 만료된 Access Token</li>
 * </ul>
 *
 * <p>인증에 성공했지만 접근 권한이 없는 403 응답은
 * {@link GatewayAccessDeniedHandler}가 처리합니다.</p>
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
     * <p>Spring Security의 인증 예외가 여러 단계로 감싸져 전달될 수 있으므로
     * 최상위 예외만 확인하지 않고 원인 체인을 순회합니다.</p>
     *
     * TODO 후속 개선 항목 : 오류 설명 문자열 expired에 의존하지 않도록
     * 커스텀 JWT Validator 또는 Decoder 적용 여부 검토
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
