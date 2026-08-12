package com.sixro.logistics.auth.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User Service의 오류 응답을 Auth Service 오류 코드로 변환하는
 * 내부 API 오류 계약을 검증하는 단위 테스트입니다.
 */
class UserServiceErrorMapperTest {

    private final UserServiceErrorMapper errorMapper =
            new UserServiceErrorMapper(new ObjectMapper());

    @ParameterizedTest
    @MethodSource("userErrorMappings")
    @DisplayName("User Service 세부 오류 코드를 Auth 오류 코드로 변환한다")
    void convertUserCreateException_mapsUserErrorCode(
            String userErrorCode,
            AuthErrorCode expectedErrorCode
    ) {
        FeignException feignException = feignException(
                409,
                errorBody(userErrorCode)
        );

        AuthException result =
                errorMapper.convertUserCreateException(feignException);

        assertThat(result.getErrorCode()).isEqualTo(expectedErrorCode);
        assertThat(result.getCause()).isSameAs(feignException);
    }

    @Test
    @DisplayName("오류 응답을 파싱할 수 없으면 HTTP 400 상태로 변환한다")
    void convertUserCreateException_invalidBody_fallsBackTo400() {
        FeignException feignException = feignException(
                400,
                "not-json"
        );

        AuthException result =
                errorMapper.convertUserCreateException(feignException);

        assertThat(result.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_SIGN_UP_REQUEST);
    }

    @Test
    @DisplayName("세부 코드가 없는 HTTP 409 응답은 중복 사용자 fallback으로 변환한다")
    void convertUserCreateException_withoutCode_fallsBackTo409() {
        FeignException feignException = feignException(
                409,
                "{}"
        );

        AuthException result =
                errorMapper.convertUserCreateException(feignException);

        assertThat(result.getErrorCode())
                .isEqualTo(AuthErrorCode.DUPLICATE_USER);
    }

    @Test
    @DisplayName("그 외 서비스 오류는 User Service 통신 실패로 변환한다")
    void convertUserCreateException_serverError_mapsCommunicationFailure() {
        FeignException feignException = feignException(
                503,
                ""
        );

        AuthException result =
                errorMapper.convertUserCreateException(feignException);

        assertThat(result.getErrorCode())
                .isEqualTo(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
    }

    private static Stream<Arguments> userErrorMappings() {
        return Stream.of(
                Arguments.of("U004", AuthErrorCode.INVALID_AFFILIATION),
                Arguments.of("U012", AuthErrorCode.INVALID_AFFILIATION),
                Arguments.of("U010", AuthErrorCode.DUPLICATE_USERNAME),
                Arguments.of("U011", AuthErrorCode.DUPLICATE_SLACK_ID),
                Arguments.of(
                        "U013",
                        AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED
                )
        );
    }

    private String errorBody(String code) {
        return """
                {
                  "success": false,
                  "status": 409,
                  "code": "%s",
                  "message": "test error"
                }
                """.formatted(code);
    }

    private FeignException feignException(
            int status,
            String body
    ) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://user-service/api/v1/internal/users",
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );

        Response response = Response.builder()
                .status(status)
                .reason("test error")
                .request(request)
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();

        return FeignException.errorStatus(
                "UserServiceClient#createUser",
                response
        );
    }
}
