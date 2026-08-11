package com.sixro.logistics.user.infrastructure.client.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Hub 및 Company 소속 검증용 FeignClient 설정입니다.
 *
 * <p>소속 서비스 장애로 User Service 요청이 장시간 대기하지 않도록
 * 연결 및 응답 Timeout을 제한합니다.</p>
 *
 * <p>현재 설정은 Timeout만 적용하며 Retry와 Circuit Breaker는
 * 별도 정책으로 적용합니다.</p>
 */
public class AffiliationClientConfig {

    /**
     * 소속 서비스 호출에 사용할 연결 및 응답 Timeout을 설정합니다.
     *
     * @param connectTimeout 대상 서비스와 연결을 맺기까지의 제한 시간
     * @param readTimeout    연결 후 응답을 기다리는 제한 시간
     */
    @Bean
    public Request.Options affiliationRequestOptions(
            @Value(
                    "${affiliation-client.connect-timeout:2s}"
            )
            Duration connectTimeout,

            @Value(
                    "${affiliation-client.read-timeout:3s}"
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