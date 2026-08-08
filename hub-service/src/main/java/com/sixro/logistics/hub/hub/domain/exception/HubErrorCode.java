package com.sixro.logistics.hub.hub.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubErrorCode implements ErrorCode {
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "해당 허브를 찾을 수 없습니다."),
    DUPLICATE_HUB_NAME(HttpStatus.CONFLICT, "H002", "이미 존재하는 허브 이름입니다."),
    INVALID_HUB_STATUS(HttpStatus.BAD_REQUEST, "H003", "유효하지 않은 허브 상태입니다."),
    INVALID_MAX_CAPACITY(HttpStatus.BAD_REQUEST, "H004", "허브 최대 물동량은 최소 10,000 이상이어야 합니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "H005", "허브 상태를 해당 단계로 변경할 수 없습니다."),
    INVALID_LOCATION_BOUNDS(HttpStatus.BAD_REQUEST, "H006", "허브 좌표가 대한민국 영토 범위를 벗어났습니다."),
    INVALID_ADDRESS(HttpStatus.BAD_REQUEST, "H007", "필수 주소 값(우편번호, 도로명 주소)이 누락되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}