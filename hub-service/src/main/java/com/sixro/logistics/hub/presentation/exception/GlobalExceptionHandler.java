package com.sixro.logistics.hub.presentation.exception;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BaseException 예외 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorResponse response = ErrorResponse.from(e.getErrorCode());

        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    // DTO 유효성 검사 실패 예외 처리 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorResponse response = new ErrorResponse(
                CommonErrorCode.VALIDATION_FAILED.getStatus().value(),
                CommonErrorCode.VALIDATION_FAILED.getCode(),
                e.getBindingResult().getAllErrors().get(0).getDefaultMessage(),
                java.time.LocalDateTime.now()
        );
        return new ResponseEntity<>(response, CommonErrorCode.VALIDATION_FAILED.getStatus());
    }

    // (@Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {

        ErrorResponse response = new ErrorResponse(
                CommonErrorCode.VALIDATION_FAILED.getStatus().value(),
                CommonErrorCode.VALIDATION_FAILED.getCode(),
                e.getConstraintViolations().iterator().next().getMessage(),
                java.time.LocalDateTime.now()
        );
        return new ResponseEntity<>(response, CommonErrorCode.VALIDATION_FAILED.getStatus());
    }

    // 쿼리 파라미터 및 경로 변수 타입 불일치 예외 처리
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e
    ) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.INVALID_PARAMETER);
        return new ResponseEntity<>(response, CommonErrorCode.INVALID_PARAMETER.getStatus());
    }

    // 지원하지 않는 HTTP Method 예외 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.METHOD_NOT_ALLOWED);
        return new ResponseEntity<>(response, CommonErrorCode.METHOD_NOT_ALLOWED.getStatus());
    }

    // JSON 파싱 오류 예외 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.INVALID_REQUEST);
        return new ResponseEntity<>(response, CommonErrorCode.INVALID_REQUEST.getStatus());
    }

    // 그 외 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Uncaught Internal Server Error occurred: ", e);
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }
}