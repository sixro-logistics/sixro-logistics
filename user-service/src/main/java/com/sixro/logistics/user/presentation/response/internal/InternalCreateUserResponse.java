package com.sixro.logistics.user.presentation.response.internal;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Auth Service의 회원가입 요청 처리 후
 * 생성된 사용자 정보를 반환하는 내부 응답 DTO입니다.
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