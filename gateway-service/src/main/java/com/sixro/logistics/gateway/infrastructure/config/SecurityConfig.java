package com.sixro.logistics.gateway.infrastructure.config;

import com.sixro.logistics.gateway.application.security.SessionValidationService;
import com.sixro.logistics.gateway.application.security.TokenBlacklistService;
import com.sixro.logistics.gateway.infrastructure.exception.GatewayErrorResponseWriter;
import com.sixro.logistics.gateway.infrastructure.filter.AccessTokenBlacklistWebFilter;
import com.sixro.logistics.gateway.infrastructure.filter.JwtHeaderRelayWebFilter;
import com.sixro.logistics.gateway.infrastructure.filter.SessionValidationWebFilter;
import com.sixro.logistics.gateway.infrastructure.security.GatewayAccessDeniedHandler;
import com.sixro.logistics.gateway.infrastructure.security.GatewayAuthenticationEntryPoint;
import com.sixro.logistics.gateway.infrastructure.security.JwtRoleGrantedAuthoritiesConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;


/**
 * API Gateway의 WebFlux 보안 정책과 보안 필터 순서를 구성합니다.
 *
 * <p>Spring Security OAuth2 Resource Server를 통해 JWT 자체를 검증하고,
 * 이후 Redis에 저장된 Access Token blacklist와
 * 현재 로그인 Session 상태를 추가로 검증합니다.</p>
 *
 * JWT 인증과 URL 기반 1차 Role 인가를 설정
 *
 * <p>본인 여부, 담당 허브, 소속 업체, 주문·배송 관계 등의
 * 도메인 정보가 필요한 최종 인가는 각 Downstream Service에서
 * 다시 검증해야 합니다.</p>
 *
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String MASTER_ADMIN = "MASTER_ADMIN";

    private static final String HUB_ADMIN = "HUB_ADMIN";

    private static final String DELIVERY_MANAGER = "DELIVERY_MANAGER";

    private static final String COMPANY_MANAGER = "COMPANY_MANAGER";

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
     * 담당 허브, 소속 업체, 리소스 소유자, 본인 여부 등의
     * 세부 인가는 각 Downstream Service에서 최종 검증합니다.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GatewayAuthenticationEntryPoint authenticationEntryPoint,
            GatewayAccessDeniedHandler accessDeniedHandler,
            AccessTokenBlacklistWebFilter accessTokenBlacklistWebFilter,
            SessionValidationWebFilter sessionValidationWebFilter,
            JwtHeaderRelayWebFilter jwtHeaderRelayWebFilter,
            Converter<Jwt, Mono<AbstractAuthenticationToken>>
                    jwtAuthenticationConverter
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

                        // 로그아웃은 Access Token이 필요합니다.
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/logout"
                        )
                        .authenticated()

                        // Gateway 상태 확인 API
                        .pathMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        // Internal API는 Gateway를 통해 노출하지 않습니다.
                        .pathMatchers(
                                "/api/v1/internal/**"
                        )
                        .denyAll()

                        /*
                         * =====================================================
                         * User
                         * =====================================================
                         */
                        // MASTER 사용자 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/users"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 가입 승인 / 거절
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/users/*/approve",
                                "/api/v1/users/*/reject"
                        )
                        .hasAnyRole(MASTER_ADMIN, HUB_ADMIN)

                        // 본인 또는 MASTER 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/users/*"
                        )
                        .authenticated()

                        // 내 정보 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/me"
                        )
                        .authenticated()

                        // 사용자 단건 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 사용자 목록 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/users"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 사용자 비활성화
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/users/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        /*
                         * =====================================================
                         * Hub
                         * =====================================================
                         */

                        // 허브 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/hubs"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 허브 수정
                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/v1/hubs/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 허브 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/hubs/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 허브 상태 변경
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/hubs/*/status"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 허브 조회·검색
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/hubs",
                                "/api/v1/hubs/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Hub Route
                         * =====================================================
                         */

                        // 스케줄러 호출은 Gateway가 아닌 내부 호출로 처리
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/hub-routes"
                        )
                        .hasRole(MASTER_ADMIN)

                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/v1/hub-routes/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/hub-routes/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/hub-routes",
                                "/api/v1/hub-routes/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Delivery Manager
                         * =====================================================
                         */

                        // 배송 담당자 등록
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/delivery-managers"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 배송 담당자 목록 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/delivery-managers"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 배송 담당자 단건 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/delivery-managers/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                DELIVERY_MANAGER
                        )

                        // 배송 담당자 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/delivery-managers/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 배송 담당자 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/delivery-managers/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        /*
                         * =====================================================
                         * Delivery Route
                         * =====================================================
                         */

                        // 배송 경로 상태 변경
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/delivery-routes/*/status"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                DELIVERY_MANAGER
                        )

                        // 허브 배송 담당자 배정·변경
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/delivery-routes/*/manager"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 배송 경로 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/delivery-routes",
                                "/api/v1/delivery-routes/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Delivery
                         * =====================================================
                         */

                        // 배송 상태 변경
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/deliveries/*/status"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                DELIVERY_MANAGER
                        )

                        // 업체 배송 담당자 배정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/deliveries/*/manager"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 배송 정보 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/deliveries/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        // 배송 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/deliveries/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 배송 단건·추적·목록 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/deliveries",
                                "/api/v1/deliveries/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Order
                         * =====================================================
                         */

                        // 주문 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/orders"
                        )
                        .authenticated()

                        // 주문 상태 변경·취소
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/orders/*/status"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 주문 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/orders/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 주문 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/orders/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 주문 단건·목록 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/orders",
                                "/api/v1/orders/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Inventory
                         * =====================================================
                         */

                        // 재고 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/inventories"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                COMPANY_MANAGER
                        )

                        // 재고 입고
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/inventories/*/stock"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                COMPANY_MANAGER
                        )

                        // 재고 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/inventories/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 재고 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/inventories/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        /*
                         * =====================================================
                         * Company
                         * =====================================================
                         */

                        // 업체 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/companies"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 업체 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/companies/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                COMPANY_MANAGER
                        )

                        // 업체 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/companies/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 업체 목록·상세 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/companies",
                                "/api/v1/companies/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Product
                         * =====================================================
                         */

                        // 상품 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/products"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                COMPANY_MANAGER
                        )

                        // 상품 수정
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/products/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN,
                                COMPANY_MANAGER
                        )

                        // 상품 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/products/*"
                        )
                        .hasAnyRole(
                                MASTER_ADMIN,
                                HUB_ADMIN
                        )

                        // 상품 목록·상세 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/products",
                                "/api/v1/products/**"
                        )
                        .authenticated()

                        /*
                         * =====================================================
                         * Notification / Slack
                         * =====================================================
                         */

                        // Slack 메시지 수정
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/slack/messages/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        // Slack 메시지 생성
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/slack/messages"
                        )
                        .authenticated()

                        // Slack 메시지 삭제
                        .pathMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/slack/messages/*"
                        )
                        .hasRole(MASTER_ADMIN)

                        // Slack 메시지 조회
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/slack/messages",
                                "/api/v1/slack/messages/**"
                        )
                        .hasRole(MASTER_ADMIN)

                        /*
                         * =====================================================
                         * Notification / AI
                         * =====================================================
                         */

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/ai/requests",
                                "/api/v1/ai/requests/**"
                        )
                        .hasRole(MASTER_ADMIN)

                        /*
                         * 명시되지 않은 API는 최소한 인증을 요구합니다.
                         * 세부 권한은 Downstream Service에서 검증합니다.
                         */
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
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
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
                .addFilterAfter(
                        jwtHeaderRelayWebFilter,
                        SecurityWebFiltersOrder.AUTHORIZATION
                )

                .build();
    }

    /**
     * JWT의 role Claim을 Spring Security의 ROLE_* Authority로 변환합니다.
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>>
    jwtAuthenticationConverter() {

        ReactiveJwtAuthenticationConverter converter =
                new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                new JwtRoleGrantedAuthoritiesConverter()
        );

        return converter;
    }
}