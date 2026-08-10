package com.sixro.logistics.hub.common.exception;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    // BaseException 예외 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException e,
            HttpServletRequest request
    ) {
        log.warn("BaseException: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        ErrorResponse response = ErrorResponse.from(e.getErrorCode(), getRequestId(request));

        return new ResponseEntity<>(response, e.getErrorCode().getStatus());
    }

    // DTO 유효성 검사 실패 예외 처리 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("MethodArgumentNotValidException: {}", errorMessage);
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.VALIDATION_FAILED, getRequestId(request));

        return new ResponseEntity<>(response, CommonErrorCode.VALIDATION_FAILED.getStatus());
    }

    // (@Validated - 파라미터 유효성 검사 실패)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("ConstraintViolationException: {}", errorMessage);
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.VALIDATION_FAILED, getRequestId(request));
        return new ResponseEntity<>(response, CommonErrorCode.VALIDATION_FAILED.getStatus());
    }

    // 쿼리 파라미터 및 경로 변수 타입 불일치 예외 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.INVALID_PARAMETER, getRequestId(request));
        return new ResponseEntity<>(response, CommonErrorCode.INVALID_PARAMETER.getStatus());
    }

    // 지원하지 않는 HTTP Method 예외 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.METHOD_NOT_ALLOWED, getRequestId(request));
        return new ResponseEntity<>(response, CommonErrorCode.METHOD_NOT_ALLOWED.getStatus());
    }

    // JSON 파싱 오류 예외 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.INVALID_REQUEST, getRequestId(request));
        return new ResponseEntity<>(response, CommonErrorCode.INVALID_REQUEST.getStatus());
    }

    // 그 외 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("Uncaught Internal Server Error occurred: ", e);
        ErrorResponse response = ErrorResponse.from(CommonErrorCode.INTERNAL_SERVER_ERROR, getRequestId(request));
        return new ResponseEntity<>(response, CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }

    private String getRequestId(HttpServletRequest request) {
        return request.getHeader(REQUEST_ID_HEADER);
    }
}