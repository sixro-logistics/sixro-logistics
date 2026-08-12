package com.sixro.logistics.auth.application.dto;

import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;

import java.util.UUID;

public record AdminCreateUserResult(
        UUID userId,
        String username,
        UserRole role,
        UserStatus userStatus
) {
}