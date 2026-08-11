package com.sixro.logistics.user.infrastructure.client.response;

import java.util.UUID;

/**
 * Hub Service 내부 허브 조회 응답의 data 구조입니다.
 *
 * Hub Service 모듈의 DTO를 직접 참조하지 않고,
 * User Service 내부에 동일한 JSON 계약을 정의합니다.
 */
public record InternalHubResponse(UUID hubId, String hubName) {

}