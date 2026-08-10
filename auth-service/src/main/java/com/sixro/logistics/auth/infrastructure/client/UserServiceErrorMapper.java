package com.sixro.logistics.auth.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.common.core.exception.ErrorResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceErrorMapper {

    private final ObjectMapper objectMapper;

    public AuthException convertUserCreateException(
            FeignException exception
    ) {
        ErrorResponse errorResponse =
                parseErrorResponse(exception);

        if (errorResponse == null
                || errorResponse.code() == null) {
            return convertByStatus(exception);
        }

        return switch (errorResponse.code()) {
            case "U004", "U012" ->
                    new AuthException(
                            AuthErrorCode.INVALID_AFFILIATION,
                            exception
                    );

            case "U010" ->
                    new AuthException(
                            AuthErrorCode.DUPLICATE_USERNAME,
                            exception
                    );

            case "U011" ->
                    new AuthException(
                            AuthErrorCode.DUPLICATE_SLACK_ID,
                            exception
                    );

            /*
             * Hub 또는 Company Service 장애로
             * User Service가 소속을 검증하지 못한 경우입니다.
             */
            case "U013" ->
                    new AuthException(
                            AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED,
                            exception
                    );

            default -> convertByStatus(exception);
        };
    }

    private AuthException convertByStatus(
            FeignException exception
    ) {
        return switch (exception.status()) {
            case 400 -> new AuthException(
                    AuthErrorCode.INVALID_SIGN_UP_REQUEST,
                    exception
            );

            case 409 -> new AuthException(
                    AuthErrorCode.DUPLICATE_USER,
                    exception
            );

            default -> new AuthException(
                    AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED,
                    exception
            );
        };
    }

    private ErrorResponse parseErrorResponse(
            FeignException exception
    ) {
        try {
            String responseBody = exception.contentUTF8();

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            return objectMapper.readValue(
                    responseBody,
                    ErrorResponse.class
            );

        } catch (Exception parseException) {
            log.warn(
                    "Failed to parse User Service error response. status={}",
                    exception.status(),
                    parseException
            );

            return null;
        }
    }
}