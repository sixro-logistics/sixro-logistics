package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeactivateUserResponse(
        UUID userId,
        LocalDateTime deletedAt
) {

    public static DeactivateUserResponse from(UserResult result) {
        return new DeactivateUserResponse(
                result.userId(),
                result.deletedAt()
        );
    }
}