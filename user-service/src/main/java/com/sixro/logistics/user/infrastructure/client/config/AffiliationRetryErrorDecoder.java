package com.sixro.logistics.user.infrastructure.client.config;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import java.util.Date;

/**
 * 소속 검증용 Hub/Company Service 호출에서 발생한
 * 일시적인 서버 오류를 Feign 재시도 대상으로 변환합니다.
 *
 * <p>Feign의 기본 ErrorDecoder는 일반적인 5xx 응답을
 * RetryableException으로 변환하지 않기 때문에,
 * Retryer가 설정되어 있어도 5xx 응답은 재시도되지 않습니다.</p>
 *
 * <p>이 Decoder는 조회 전용 GET API에만 적용해야 합니다.
 * 생성·수정과 같은 비멱등 요청에 적용하면 중복 처리가 발생할 수 있습니다.</p>
 */
public class AffiliationRetryErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder =
            new ErrorDecoder.Default();

    @Override
    public Exception decode(
            String methodKey,
            Response response
    ) {
        int status = response.status();

        /*
         * 모든 HTTP 5xx 응답을 RetryableException으로 변환합니다.
         * Retryer는 RetryableException이 발생한 경우에만
         * 설정된 횟수와 간격에 따라 호출을 재시도합니다.
         */
        if (status >= 500 && status < 600) {
            FeignException cause =
                    FeignException.errorStatus(
                            methodKey,
                            response
                    );

            Request request = response.request();

            return new RetryableException(
                    status,
                    "소속 서비스에서 서버 오류가 발생했습니다. "
                            + "status=" + status
                            + ", methodKey=" + methodKey,
                    request.httpMethod(),
                    cause,
                    (Date) null,
                    request
            );
        }

        /*
         * 4xx 응답 등 재시도 대상이 아닌 오류는
         * Feign의 기본 오류 변환 정책을 사용합니다.
         *
         * 따라서 404/410은 AffiliationValidationService에서
         * AFFILIATION_NOT_FOUND로 변환할 수 있습니다.
         */
        return defaultErrorDecoder.decode(
                methodKey,
                response
        );
    }
}