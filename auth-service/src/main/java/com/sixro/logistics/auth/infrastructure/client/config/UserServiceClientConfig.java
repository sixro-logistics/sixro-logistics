package com.sixro.logistics.auth.infrastructure.client.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Auth Service에서 User Service를 호출할 때 사용하는
 * OpenFeign Timeout 설정입니다.
 *
 * <p>로그인, 회원가입, 토큰 재발급 과정에서 User Service 장애로
 * 요청이 장시간 대기하지 않도록 연결 및 응답 시간을 제한합니다.</p>
 *
 * <p>현재는 Timeout만 설정하며 Retry와 Circuit Breaker는
 * 별도 정책으로 적용합니다.</p>
 */
public class UserServiceClientConfig {

    @Bean
    public Request.Options userServiceRequestOptions(
            @Value(
                    "${user-service-client.connect-timeout:2s}"
            )
            Duration connectTimeout,

            @Value(
                    "${user-service-client.read-timeout:3s}"
            )
            Duration readTimeout
    ) {
        return new Request.Options(
                connectTimeout,
                readTimeout,
                false
        );
    }
}