package com.sixro.logistics.auth.presentation.response;

import com.sixro.logistics.auth.application.dto.AdminCreateUserResult;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;

import java.util.UUID;

public record AdminCreateUserResponse(
        UUID userId,
        String username,
        UserRole role,
        UserStatus userStatus
) {

    public static AdminCreateUserResponse from(
            AdminCreateUserResult result
    ) {
        return new AdminCreateUserResponse(
                result.userId(),
                result.username(),
                result.role(),
                result.userStatus()
        );
    }
}