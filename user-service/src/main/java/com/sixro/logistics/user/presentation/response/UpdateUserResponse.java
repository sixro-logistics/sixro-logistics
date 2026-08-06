package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;

import java.util.UUID;

public record UpdateUserResponse(
        UUID userId,
        String slackId,
        AffiliationType affiliationType,
        UUID affiliationId
) {

    public static UpdateUserResponse from(UserResult result) {
        return new UpdateUserResponse(
                result.userId(),
                result.slackId(),
                result.affiliationType(),
                result.affiliationId()
        );
    }
}