package com.sixro.logistics.common.core.exception;

import java.time.LocalDateTime;

/**
 * API 오류 응답의 공통 형식입니다.
 *
 * @param success   요청 성공 여부
 * @param status    HTTP 상태 코드
 * @param code      서비스 오류 코드
 * @param message   오류 메시지
 * @param timestamp 응답 생성 시각
 */
public record ErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now()
        );
    }
}