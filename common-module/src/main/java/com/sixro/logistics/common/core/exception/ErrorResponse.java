package com.sixro.logistics.common.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * API 오류 응답의 공통 형식입니다.
 *
 * @param success   요청 성공 여부
 * @param status    HTTP 상태 코드
 * @param code      서비스 오류 코드
 * @param message   오류 메시지
 * @param requestId 요청 추적 식별자
 * @param timestamp 응답 생성 시각
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        String requestId,
        LocalDateTime timestamp
) {

    /**
     * Request ID 없이 공통 오류 응답을 생성합니다.
     *
     * <p>필드 생성 로직의 중복을 방지하기 위해
     * requestId를 포함하는 생성 메서드로 위임합니다.</p>
     */
    public static ErrorResponse from(
            ErrorCode errorCode
    ) {
        return from(errorCode, null);
    }

    /**
     * Request ID를 포함한 공통 오류 응답을 생성합니다.
     *
     * <p>Gateway처럼 요청 추적(Request ID)이 필요한 서비스에서 사용합니다.</p>
     */
    public static ErrorResponse from(
            ErrorCode errorCode,
            String requestId
    ) {
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                requestId,
                LocalDateTime.now()
        );
    }
}