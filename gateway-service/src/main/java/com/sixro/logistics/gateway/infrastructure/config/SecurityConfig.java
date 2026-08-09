package com.sixro.logistics.gateway.infrastructure.config;

import com.sixro.logistics.gateway.application.security.SessionValidationService;
import com.sixro.logistics.gateway.application.security.TokenBlacklistService;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import com.sixro.logistics.gateway.infrastructure.filter.AccessTokenBlacklistWebFilter;
import com.sixro.logistics.gateway.infrastructure.filter.JwtHeaderRelayWebFilter;
import com.sixro.logistics.gateway.infrastructure.filter.SessionValidationWebFilter;
import com.sixro.logistics.gateway.infrastructure.security.GatewayAccessDeniedHandler;
import com.sixro.logistics.gateway.infrastructure.security.GatewayAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;

/**
 * API Gateway의 WebFlux 보안 정책과 보안 필터 순서를 구성합니다.
 *
 * <p>Spring Security OAuth2 Resource Server를 통해 JWT 자체를 검증하고,
 * 이후 Redis에 저장된 Access Token blacklist와
 * 현재 로그인 Session 상태를 추가로 검증합니다.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Access Token 블랙리스트 필터를 Security Filter Chain에
     * 명시적인 순서로 등록하기 위해 Bean으로 생성합니다.
     */
    @Bean
    public AccessTokenBlacklistWebFilter accessTokenBlacklistWebFilter(
            TokenBlacklistService tokenBlacklistService,
            GatewayErrorResponseWriter errorResponseWriter
    ) {
        return new AccessTokenBlacklistWebFilter(
                tokenBlacklistService,
                errorResponseWriter
        );
    }

    /**
     * JWT의 sessionId와 Redis에 저장된 현재 사용자 Session을
     * 비교하는 필터를 생성합니다.
     */
    @Bean
    public SessionValidationWebFilter sessionValidationWebFilter(
            SessionValidationService sessionValidationService,
            GatewayErrorResponseWriter errorResponseWriter
    ) {
        return new SessionValidationWebFilter(
                sessionValidationService,
                errorResponseWriter
        );
    }

    /**
     * JWT Claim을 내부 서비스용 Header로 변환하는 필터를 생성합니다.
     */
    @Bean
    public JwtHeaderRelayWebFilter jwtHeaderRelayWebFilter(
            GatewayErrorResponseWriter errorResponseWriter
    ) {
        return new JwtHeaderRelayWebFilter(errorResponseWriter);
    }

    /**
     * Gateway의 공개 API, 인증 필요 API, 예외 처리 및
     * 커스텀 보안 필터 실행 순서를 구성합니다.
     *
     * <p>현재는 공개 API를 제외한 모든 요청에 JWT 인증만 적용합니다.
     * 역할별 1차 인가 정책은 User, Hub, Delivery API 권한 규칙이
     * 확정된 이후 추가합니다.</p>
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GatewayAuthenticationEntryPoint authenticationEntryPoint,
            GatewayAccessDeniedHandler accessDeniedHandler,
            AccessTokenBlacklistWebFilter accessTokenBlacklistWebFilter,
            SessionValidationWebFilter sessionValidationWebFilter,
            JwtHeaderRelayWebFilter jwtHeaderRelayWebFilter
    ) {
        /*
         * JWT 기반 Stateless API이므로
         * 세션 기반 인증에서 사용하는 보안 기능을 비활성화합니다.
         *
         * - CSRF, Form Login, HTTP Basic, 기본 Logout
         */
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // 브라우저의 CORS 사전 요청은 인증 없이 허용
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // 회원가입, 로그인, 토큰 재발급은 인증 없이 허용
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/reissue"
                        )
                        .permitAll()

                        // Gateway 상태 확인 API
                        .pathMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        // 그 외 모든 요청은 유효한 JWT 인증 필요
                        // TODO User Service 개발 후 Role 정책 확정되면 수정범위
                        //  - MASTER_ADMIN 등의 Role 기반 1차 인가를 추가
                        .anyExchange()
                        .authenticated()
                )

                // Spring Security 인가 단계에서 발생한 401/403 응답 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Bearer Token의 RSA 서명, issuer, expiration, tokenType == ACCESS 를 검증
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                /*
                 * JWT 인증 결과가 ServerWebExchange Principal에 연결된 이후
                 * JTI를 이용해 Redis 블랙리스트를 검사합니다.
                 * 로그아웃된 Access Token의 재사용을 차단합니다.
                 */
                .addFilterAfter(
                        accessTokenBlacklistWebFilter,
                        SecurityWebFiltersOrder.SECURITY_CONTEXT_SERVER_WEB_EXCHANGE
                )

                /*
                 * blacklist 검증을 통과한 Access Token에 대해
                 * JWT sessionId와 Redis의 현재 Session을 비교합니다.
                 *
                 * 새로운 로그인으로 sessionId가 교체된 경우
                 * 이전 Access Token을 차단합니다.
                 */
                .addFilterAfter(
                        sessionValidationWebFilter,
                        SecurityWebFiltersOrder
                                .SECURITY_CONTEXT_SERVER_WEB_EXCHANGE
                )

                /*
                 * 인증·인가가 완료된 요청의 JWT Claim을
                 * 내부 서비스에서 사용할 Header로 변환합니다.
                 *
                 * 클라이언트가 전달한 내부 인증 Header는 제거한 뒤
                 * Gateway에서 검증한 JWT Claim으로 다시 설정합니다.
                 */
                /* TODO 아직 역할별 hasRole() 규칙이 없으므로 실질적으로는 인증된 요청에 헤더를 추가 */
                .addFilterAfter(
                        jwtHeaderRelayWebFilter,
                        SecurityWebFiltersOrder.AUTHORIZATION
                )

                .build();
    }
}