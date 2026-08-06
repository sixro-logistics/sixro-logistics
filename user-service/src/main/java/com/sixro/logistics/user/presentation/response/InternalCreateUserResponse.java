package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 내부 사용자 생성 결과를 Auth Service에 반환하는 응답 DTO입니다.
 */
public record InternalCreateUserResponse(
        UUID userId,
        String username,
        UserRole role,
        UserStatus userStatus
) {

    public static InternalCreateUserResponse from(UserResult result) {
        return new InternalCreateUserResponse(
                result.userId(),
                result.username(),
                result.role(),
                result.userStatus()
        );
    }
}