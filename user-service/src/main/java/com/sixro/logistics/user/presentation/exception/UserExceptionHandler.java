package com.sixro.logistics.user.presentation.exception;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * User Service에서 발생한 예외를 공통 오류 응답 형식으로 변환합니다.
 */
@Slf4j
@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException exception
    ) {
        log.warn(
                "User business exception. code={}, message={}",
                exception.getErrorCode().getCode(),
                exception.getMessage()
        );

        ErrorResponse response = ErrorResponse.from(
                exception.getErrorCode()
        );

        return ResponseEntity
                .status(exception.getErrorCode().getStatus())
                .body(response);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception exception
    ) {
        log.warn(
                "User request validation failed: {}",
                exception.getMessage()
        );

        ErrorResponse response = ErrorResponse.from(
                CommonErrorCode.VALIDATION_FAILED
        );

        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_FAILED.getStatus())
                .body(response);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidParameter(
            Exception exception
    ) {
        log.warn(
                "Invalid user request parameter: {}",
                exception.getMessage()
        );

        ErrorResponse response = ErrorResponse.from(
                CommonErrorCode.INVALID_PARAMETER
        );

        return ResponseEntity
                .status(CommonErrorCode.INVALID_PARAMETER.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "Unexpected error occurred in User Service.",
                exception
        );

        ErrorResponse response = ErrorResponse.from(
                CommonErrorCode.INTERNAL_SERVER_ERROR
        );

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(response);
    }
}