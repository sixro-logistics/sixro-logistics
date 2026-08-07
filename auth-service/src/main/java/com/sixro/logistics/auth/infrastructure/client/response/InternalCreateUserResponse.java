package com.sixro.logistics.auth.infrastructure.client.response;

import com.sixro.logistics.auth.domain.model.UserStatus;

import java.util.UUID;

/**
 * User Service의 내부 사용자 생성 API 응답 데이터입니다.
 *
 * @param userId 생성된 사용자 식별자
 * @param username 생성된 사용자명
 * @param userStatus 최초 사용자 상태
 */
public record InternalCreateUserResponse(

        UUID userId,

        String username,

        UserStatus userStatus

) {
}