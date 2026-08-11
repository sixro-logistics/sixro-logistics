package com.sixro.logistics.user.infrastructure.client.config;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소속 검증 FeignClient의 오류 응답 변환 정책을 검증하는 단위 테스트입니다.
 *
 * <p>5xx 응답이 재시도 가능한 RetryableException으로 변환되는지,
 * 4xx 응답은 재시도 대상에서 제외되는지 확인합니다.</p>
 *
 * <p>테스트에 사용되는 URL은 Feign 요청 정보를 구성하기 위한 값이며,
 * 실제 HTTP 요청은 발생하지 않습니다.</p>
 */
class AffiliationRetryErrorDecoderTest {

    private static final String HUB_URL =
            "http://hub-service/api/v1/internal/hubs/"
                    + "85d08a26-9468-48cb-a1ef-12a352dfc253";

    private final AffiliationRetryErrorDecoder errorDecoder =
            new AffiliationRetryErrorDecoder();

    @ParameterizedTest
    @ValueSource(ints = {500, 501, 502, 503, 504, 599})
    @DisplayName("모든 5xx 응답은 RetryableException으로 변환한다")
    void decode_serverError_returnsRetryableException(int status) {
        Response response = response(status, Request.HttpMethod.GET);

        Exception exception = errorDecoder.decode(
                "HubServiceClient#getHub",
                response
        );

        assertThat(exception)
                .isInstanceOf(RetryableException.class);

        RetryableException retryableException =
                (RetryableException) exception;

        assertThat(retryableException.status()).isEqualTo(status);
        assertThat(retryableException.request().httpMethod())
                .isEqualTo(Request.HttpMethod.GET);
        assertThat(retryableException.getCause())
                .isInstanceOf(FeignException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 410, 429})
    @DisplayName("4xx 응답은 재시도 대상으로 변환하지 않는다")
    void decode_clientError_doesNotReturnRetryableException(int status) {
        Response response = response(status, Request.HttpMethod.GET);

        Exception exception = errorDecoder.decode(
                "CompanyServiceClient#getCompany",
                response
        );

        assertThat(exception)
                .isInstanceOf(FeignException.class)
                .isNotInstanceOf(RetryableException.class);
        assertThat(((FeignException) exception).status())
                .isEqualTo(status);
    }

    @Test
    @DisplayName("재시도 예외에 원본 요청 정보가 유지된다")
    void decode_preservesOriginalRequest() {
        Response response = response(
                503,
                Request.HttpMethod.GET
        );

        RetryableException exception =
                (RetryableException) errorDecoder.decode(
                        "HubServiceClient#getHub",
                        response
                );

        assertThat(exception.request().url())
                .isEqualTo(HUB_URL);
        assertThat(exception.request().httpMethod())
                .isEqualTo(Request.HttpMethod.GET);
    }

    private Response response(
            int status,
            Request.HttpMethod httpMethod
    ) {
        Request request = Request.create(
                httpMethod,
                HUB_URL,
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );

        return Response.builder()
                .status(status)
                .reason("test response")
                .request(request)
                .headers(Map.of())
                .build();
    }
}
