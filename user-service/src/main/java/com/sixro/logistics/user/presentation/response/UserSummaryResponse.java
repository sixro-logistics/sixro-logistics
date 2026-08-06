package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        AffiliationType affiliationType,
        UUID affiliationId,
        UserStatus userStatus
) {

    public static UserSummaryResponse from(UserResult result) {
        return new UserSummaryResponse(
                result.userId(),
                result.username(),
                result.slackId(),
                result.role(),
                result.affiliationType(),
                result.affiliationId(),
                result.userStatus()
        );
    }
}