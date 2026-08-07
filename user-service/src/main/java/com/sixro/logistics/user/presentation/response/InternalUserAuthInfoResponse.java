package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.InternalUserAuthResult;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

public record InternalUserAuthInfoResponse(
        UUID userId,
        String username,
        String encodedPassword,
        UserRole role,
        UserStatus userStatus,
        boolean deleted
) {

    public static InternalUserAuthInfoResponse from(
            InternalUserAuthResult result
    ) {
        return new InternalUserAuthInfoResponse(
                result.userId(),
                result.username(),
                result.encodedPassword(),
                result.role(),
                result.userStatus(),
                result.deleted()
        );
    }
}