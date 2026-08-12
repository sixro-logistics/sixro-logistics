package com.sixro.logistics.gateway.infrastructure.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * API Gateway의 요청 제한 기준을 정의합니다.
 */
@Configuration
public class RateLimitConfig {

    /**
     * 로그인·회원가입 등 인증 전에 호출되는 API에 사용할 KeyResolver입니다.
     *
     * <p>인증 전에는 JWT의 userId를 사용할 수 없으므로
     * 요청자의 IP 주소를 Rate Limiting 키로 사용합니다.</p>
     */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                        exchange.getRequest().getRemoteAddress()
                )
                .map(address ->
                        address.getAddress().getHostAddress()
                )
                .defaultIfEmpty("unknown");
    }
}