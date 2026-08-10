package com.sixro.logistics.common.core.response;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * API 정상 응답의 공통 형식입니다.
 *
 * @param success   요청 성공 여부
 * @param status    HTTP 상태 코드
 * @param message   응답 메시지
 * @param data      응답 데이터
 * @param timestamp 응답 생성 시각
 * @param <T>       응답 데이터 타입
 */
public record CommonResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        LocalDateTime timestamp
) {

    /**
     * 지정한 HTTP 상태와 데이터를 포함한 성공 응답을 생성합니다.
     */
    public static <T> CommonResponse<T> success(
            HttpStatus status,
            String message,
            T data
    ) {
        return new CommonResponse<>(
                true,
                status.value(),
                message,
                data,
                LocalDateTime.now()
        );
    }

    /**
     * HTTP 200 OK 성공 응답을 생성합니다.
     */
    public static <T> CommonResponse<T> success(
            String message,
            T data
    ) {
        return success(HttpStatus.OK, message, data);
    }

    /**
     * 응답 데이터가 없는 HTTP 200 OK 성공 응답을 생성합니다.
     */
    public static CommonResponse<Void> success(String message) {
        return success(HttpStatus.OK, message, null);
    }

    /**
     * HTTP 201 Created 성공 응답을 생성합니다.
     */
    public static <T> CommonResponse<T> created(
            String message,
            T data
    ) {
        return success(HttpStatus.CREATED, message, data);
    }

}