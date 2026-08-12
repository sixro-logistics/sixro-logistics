package com.sixro.logistics.auth.infrastructure.client;

import com.sixro.logistics.auth.infrastructure.client.config.AuthUserServiceFailurePredicate;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auth Service에서 User Service 호출 시 발생한 예외를
 * Circuit Breaker 장애로 기록할지 판단하는 정책을 검증합니다.
 *
 * <p>4xx 응답은 사용자 없음, 비활성 사용자, 잘못된 요청 등의
 * 비즈니스 결과이므로 Circuit Breaker 실패율에 포함하지 않습니다.</p>
 *
 * <p>5xx 응답과 네트워크 장애 등 시스템 예외는
 * User Service 장애로 판단하여 실패율에 포함합니다.</p>
 *
 * <p>테스트 URL은 Feign 예외를 만들기 위한 요청 정보이며,
 * 실제 HTTP 요청은 발생하지 않습니다.</p>
 */
class AuthUserServiceFailurePredicateTest {

    private static final String USER_SERVICE_URL =
            "http://user-service/api/v1/internal/users/"
                    + "auth-info/hubadmin1";

    private final AuthUserServiceFailurePredicate predicate =
            new AuthUserServiceFailurePredicate();

    @ParameterizedTest
    @ValueSource(ints = {
            400,
            401,
            403,
            404,
            409,
            410,
            429
    })
    @DisplayName("4xx 응답은 Circuit Breaker 장애로 기록하지 않는다")
    void test_clientError_doesNotRecordFailure(
            int status
    ) {
        FeignException exception =
                feignException(status);

        boolean result = predicate.test(exception);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            500,
            501,
            502,
            503,
            504,
            599
    })
    @DisplayName("5xx 응답은 Circuit Breaker 장애로 기록한다")
    void test_serverError_recordsFailure(
            int status
    ) {
        FeignException exception =
                feignException(status);

        boolean result = predicate.test(exception);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("FeignException이 아닌 시스템 예외는 Circuit Breaker 장애로 기록한다")
    void test_systemException_recordsFailure() {
        RuntimeException exception =
                new RuntimeException(
                        "User Service 연결 실패"
                );

        boolean result = predicate.test(exception);

        assertThat(result).isTrue();
    }

    /**
     * 지정한 HTTP 상태를 가진 FeignException을 생성합니다.
     */
    private FeignException feignException(
            int status
    ) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                USER_SERVICE_URL,
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );

        Response response = Response.builder()
                .status(status)
                .reason("test response")
                .request(request)
                .headers(Map.of())
                .build();

        return FeignException.errorStatus(
                "UserServiceClient#getAuthInfo",
                response
        );
    }
}