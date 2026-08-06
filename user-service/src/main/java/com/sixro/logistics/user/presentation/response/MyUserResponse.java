package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

public record MyUserResponse(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        AffiliationType affiliationType,
        UUID affiliationId
) {

    public static MyUserResponse from(UserResult result) {
        return new MyUserResponse(
                result.userId(),
                result.username(),
                result.slackId(),
                result.role(),
                result.affiliationType(),
                result.affiliationId()
        );
    }
}