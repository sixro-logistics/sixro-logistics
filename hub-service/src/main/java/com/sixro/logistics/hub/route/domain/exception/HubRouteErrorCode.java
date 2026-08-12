package com.sixro.logistics.hub.route.domain.exception;

import com.sixro.logistics.common.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubRouteErrorCode implements ErrorCode {
    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HR001", "해당 허브 노선을 찾을 수 없습니다."),
    SAME_ORIGIN_AND_DESTINATION(HttpStatus.BAD_REQUEST, "HR002", "출발 허브와 도착 허브는 동일할 수 없습니다."),
    INVALID_DISTANCE(HttpStatus.BAD_REQUEST, "HR003", "이동 거리는 0m 초과, 1,000,000m(1000km) 이하이어야 합니다."),
    INVALID_DURATION(HttpStatus.BAD_REQUEST, "HR004", "소요 시간은 0초 초과, 86,400초(24시간) 이하이어야 합니다."),
    INVALID_COST(HttpStatus.BAD_REQUEST, "HR005", "운임 및 통행료는 음수일 수 없습니다."),
    INVALID_ROUTE_PATH(HttpStatus.BAD_REQUEST, "HR006", "경로 형상(LineString) 데이터는 필수이며 비어있을 수 없습니다."),
    DUPLICATE_HUB_ROUTE(HttpStatus.CONFLICT, "HR007", "이미 존재하는 허브 노선입니다."),
    INVALID_LOCATION_BOUNDS(HttpStatus.BAD_REQUEST, "HR008", "좌표가 대한민국 영토 범위를 벗어났습니다."),

    // 외부 API 연동 에러코드
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "HR009", "외부 경로 API 호출 중 오류가 발생했습니다."),
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "HR010", "외부 경로 서비스가 현재 응답하지 않습니다. (서킷 오픈)"),
    EXTERNAL_API_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "HR011", "요청 횟수 제한을 초과했습니다. 잠시 후 다시 시도해주세요."),
    EXTERNAL_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "HR012", "외부 경로 API 인증에 실패했습니다."),
    EXTERNAL_API_BAD_REQUEST(HttpStatus.BAD_REQUEST, "HR013", "외부 경로 API에 잘못된 요청을 보냈습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}