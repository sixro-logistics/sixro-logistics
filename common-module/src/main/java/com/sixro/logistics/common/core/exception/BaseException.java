package com.sixro.logistics.common.core.exception;

import lombok.Getter;

import java.util.Objects;

/**
 * 공통 예외의 부모 클래스입니다.
 *
 * <p>
 * 각 서비스는 ErrorCode를 전달하여 예외를 생성하고,
 * 서비스별 ExceptionHandler에서 이를 처리합니다.
 * </p>
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(
                Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(),
                cause
        );
        this.errorCode = errorCode;
    }
}