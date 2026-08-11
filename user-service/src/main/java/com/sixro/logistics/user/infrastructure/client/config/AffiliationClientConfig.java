package com.sixro.logistics.user.infrastructure.client.config;

import feign.Request;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Hub 및 Company 소속 검증용 FeignClient 설정입니다.
 *
 * <p>소속 서비스 장애로 User Service 요청이 장시간 대기하지 않도록
 * 연결 및 응답 Timeout을 제한합니다.</p>
 *
 * <p>연결 및 응답 Timeout을 제한하고, 일시적인 네트워크 장애에
 * 대해서만 제한적으로 재시도합니다.</p>
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

    /**
     * 일시적인 연결 실패와 Timeout에 대한 재시도 정책입니다.
     *
     * maxAttempts에는 최초 호출이 포함되므로
     * 3으로 설정하면 최초 1회와 재시도 2회가 수행됩니다.
     */
    @Bean
    public Retryer affiliationRetryer(
            @Value(
                    "${affiliation-client.retry.initial-interval:200ms}"
            )
            Duration initialInterval,

            @Value(
                    "${affiliation-client.retry.max-interval:1s}"
            )
            Duration maxInterval,

            @Value(
                    "${affiliation-client.retry.max-attempts:3}"
            )
            int maxAttempts
    ) {
        return new Retryer.Default(
                initialInterval.toMillis(),
                maxInterval.toMillis(),
                maxAttempts
        );
    }
}