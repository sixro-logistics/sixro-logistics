package com.sixro.logistics.company.exception;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.ErrorCode;
import com.sixro.logistics.common.core.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ErrorCode errorCode = e.getErrorCode();

        // errorCode.getStatus() 값을 ResponseEntity.status()에 전달해야 404로 응답됩니다.
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.from(errorCode));
    }
}
