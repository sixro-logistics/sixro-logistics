package com.sixro.logistics.user.presentation.response.internal;

import com.sixro.logistics.user.application.dto.InternalUserAuthResult;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Auth Service가 로그인 및 JWT 발급에 사용하는
 * 내부 사용자 인증 정보 응답 DTO입니다.
 */
public record InternalUserAuthInfoResponse(
        UUID userId,
        String username,
        String encodedPassword,
        UserRole role,
        UserStatus userStatus
) {

    public static InternalUserAuthInfoResponse from(
            InternalUserAuthResult result
    ) {
        return new InternalUserAuthInfoResponse(
                result.userId(),
                result.username(),
                result.encodedPassword(),
                result.role(),
                result.userStatus()
        );
    }
}