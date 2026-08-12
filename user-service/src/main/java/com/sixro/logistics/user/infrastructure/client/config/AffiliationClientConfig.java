package com.sixro.logistics.user.infrastructure.client.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Hub 및 Company 소속 검증용 FeignClient 설정입니다.
 *
 * <p>소속 조회 API는 GET 방식의 멱등 요청이므로 연결 실패,
 * 응답 시간 초과 및 재시도 대상으로 변환된 5xx 응답에 대해
 * 제한적인 재시도를 적용합니다.</p>
 *
 * <p>연결 및 응답 Timeout은
 * {@code spring.cloud.openfeign.client.config}에서 관리하고,
 * 이 클래스에서는 소속 검증 전용 Retry와 ErrorDecoder만 관리합니다.</p>
 *
 * <p>이 클래스에는 {@code @Configuration}을 선언하지 않습니다.
 * HubServiceClient와 CompanyServiceClient의 configuration 속성을 통해
 * 해당 Client에만 적용하여 다른 FeignClient로 설정이 확산되는 것을 방지합니다.</p>
 */
public class AffiliationClientConfig {

    /**
     * 일시적인 연결 실패, 응답 Timeout 및 재시도 대상으로 변환된
     * 5xx 응답에 대한 재시도 정책입니다.
     *
     * <p>{@code maxAttempts}에는 최초 요청이 포함됩니다.
     * 3으로 설정하면 최초 요청 1회와 재시도 최대 2회가 수행됩니다.</p>
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
        return new Retryer.Default(
                initialIntervalMillis,
                maxIntervalMillis,
                maxAttempts
        );
    }

    /**
     * 재시도 가능한 5xx 응답을 RetryableException으로 변환합니다.
     *
     * <p>4xx 응답은 유효하지 않은 요청 또는 존재하지 않는 소속으로
     * 판단하므로 재시도 대상으로 변환하지 않습니다.</p>
     */
    @Bean
    public ErrorDecoder affiliationRetryErrorDecoder() {
        return new AffiliationRetryErrorDecoder();
    }
}