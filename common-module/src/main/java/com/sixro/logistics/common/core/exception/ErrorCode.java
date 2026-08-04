package com.sixro.logistics.common.core.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 공통 및 서비스별 오류 코드가 구현해야 하는 규약입니다.
 *
 * <p>각 서비스는 이 인터페이스를 구현한 Enum을 정의하여
 * HTTP 상태, 서비스 오류 코드, 사용자에게 전달할 메시지를 관리합니다.</p>
 */
public interface ErrorCode {

    /**
     * API 응답에 사용할 HTTP 상태입니다.
     */
    HttpStatus getStatus();

    /**
     * 클라이언트와 서버가 오류를 식별할 때 사용하는 오류 코드입니다.
     *
     * <p>예: C001, U001, A001</p>
     */
    String getCode();

    /**
     * 클라이언트에 전달할 오류 메시지입니다.
     */
    String getMessage();
}