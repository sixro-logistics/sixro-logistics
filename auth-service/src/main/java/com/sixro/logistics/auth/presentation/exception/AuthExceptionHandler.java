package com.sixro.logistics.auth.presentation.exception;

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
 * Auth Service에서 발생한 예외를 공통 오류 응답으로 변환합니다.
 *
 * <p>예상 가능한 비즈니스 예외와 요청 검증 오류는 경고 로그로 남기고,
 * 예상하지 못한 서버 오류는 스택 트레이스와 함께 오류 로그로 기록합니다.</p>
 */
// TODO(common-module): Auth/User 서비스의 중복 ExceptionHandler 로직 중
//  공통 예외 및 요청 검증 처리를 공통 모듈로 추출할지 검토
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    // Auth Service의 비즈니스 예외를 해당 HTTP 상태로 반환합니다.
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException exception
    ) {
        log.warn(
                "Auth business exception. code={}, message={}",
                exception.getErrorCode().getCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getErrorCode().getStatus())
                .body(ErrorResponse.from(
                        exception.getErrorCode()
                ));
    }

    // 요청 본문 형식과 Bean Validation 실패를 처리합니다.
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception exception
    ) {
        log.warn(
                "Auth request validation failed: {}",
                exception.getMessage()
        );

        return ResponseEntity
                .status(
                        CommonErrorCode.VALIDATION_FAILED.getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.VALIDATION_FAILED
                ));
    }

    // 누락되거나 형식이 잘못된 헤더 및 요청 파라미터를 처리합니다.
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidParameter(
            Exception exception
    ) {
        log.warn(
                "Invalid auth request parameter: {}",
                exception.getMessage()
        );

        return ResponseEntity
                .status(
                        CommonErrorCode.INVALID_PARAMETER.getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.INVALID_PARAMETER
                ));
    }

    /**
     * 별도로 처리되지 않은 예외를 내부 서버 오류로 변환합니다.
     *
     * <p>내부 예외 정보는 클라이언트에 노출하지 않습니다.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "Unexpected error occurred in Auth Service.",
                exception
        );

        return ResponseEntity
                .status(
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus()
                )
                .body(ErrorResponse.from(
                        CommonErrorCode.INTERNAL_SERVER_ERROR
                ));
    }
}