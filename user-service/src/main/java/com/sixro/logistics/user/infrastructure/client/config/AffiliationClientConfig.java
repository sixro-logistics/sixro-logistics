package com.sixro.logistics.user.infrastructure.client.config;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Hub 및 Company 소속 검증용 FeignClient 설정입니다.
 *
 * <p>소속 조회 API는 GET 방식의 멱등 요청이므로 연결 실패,
 * 응답 시간 초과 및 재시도 대상으로 변환된 5xx 응답에 대해
 * 제한적인 재시도를 적용합니다.</p>
 *
 * <p>Feign 전용 컨텍스트의 Duration 변환 문제를 방지하기 위해
 * 모든 시간 설정은 밀리초 단위의 long으로 주입합니다.</p>
 *
 * <p>이 클래스에는 @Configuration을 선언하지 않습니다.
 * HubServiceClient와 CompanyServiceClient의 configuration 속성을 통해
 * 해당 Client에만 적용하여 다른 FeignClient로 설정이 확산되는 것을 방지합니다.</p>
 */
public class AffiliationClientConfig {

    /**
     * 소속 서비스 호출에 사용할 연결 및 응답 Timeout을 설정합니다.
     */
    @Bean
    public Request.Options affiliationRequestOptions(
            @Value("${affiliation-client.connect-timeout-ms:2000}")
            long connectTimeoutMillis,

            @Value("${affiliation-client.read-timeout-ms:3000}")
            long readTimeoutMillis
    ) {
        return new Request.Options(
                Duration.ofMillis(connectTimeoutMillis),
                Duration.ofMillis(readTimeoutMillis),
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
            @Value("${affiliation-client.retry.initial-interval-ms:200}")
            long initialIntervalMillis,

            @Value("${affiliation-client.retry.max-interval-ms:1000}")
            long maxIntervalMillis,

            @Value("${affiliation-client.retry.max-attempts:3}")
            int maxAttempts
    ) {
        /*
         * maxAttempts에는 최초 요청이 포함됩니다.
         * 3이면 최초 요청 1회와 재시도 최대 2회입니다.
         */
        return new Retryer.Default(
                initialIntervalMillis,
                maxIntervalMillis,
                maxAttempts
        );
    }

    @Bean
    public ErrorDecoder affiliationRetryErrorDecoder() {
        return new AffiliationRetryErrorDecoder();
    }
}