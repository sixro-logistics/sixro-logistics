package com.sixro.logistics.auth.infrastructure.client.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Auth Service에서 User Service 내부 API를 호출할 때 사용하는
 * OpenFeign Timeout 설정입니다.
 *
 * <p>로그인, 회원가입, 토큰 재발급 과정에서 User Service 장애로
 * 요청이 장시간 대기하지 않도록 연결 및 응답 시간을 제한합니다.</p>
 *
 * <p>Feign 전용 컨텍스트에서 문자열을 Duration으로 변환할 때
 * 환경에 따라 변환 오류가 발생할 수 있으므로 설정값은 밀리초 단위의
 * long으로 주입한 뒤 Duration으로 변환합니다.</p>
 *
 * <p>현재는 Timeout만 적용합니다. 사용자 생성 POST 요청이 포함되어 있으므로
 * 이 Client 전체에 재시도를 적용하지 않습니다.</p>
 */
public class UserServiceClientConfig {

    @Bean
    public Request.Options userServiceRequestOptions(
            @Value("${user-service-client.connect-timeout-ms:2000}")
            long connectTimeoutMillis,

            @Value("${user-service-client.read-timeout-ms:3000}")
            long readTimeoutMillis
    ) {
        return new Request.Options(
                Duration.ofMillis(connectTimeoutMillis),
                Duration.ofMillis(readTimeoutMillis),
                false
        );
    }
}