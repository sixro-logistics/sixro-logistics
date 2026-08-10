package com.sixro.logistics.user.application.dto;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Auth Service 로그인 검증에 사용하는 내부 사용자 정보입니다.
 */
public record InternalUserAuthResult(
        UUID userId,
        String username,
        String encodedPassword,
        UserRole role,
        UserStatus userStatus,
        boolean deleted
) {

    public static InternalUserAuthResult from(User user) {
        return new InternalUserAuthResult(
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getUserStatus(),
                user.isDeleted()
        );
    }
}