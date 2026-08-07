package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.InternalDeliveryUserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

public record InternalDeliveryUserResponse(
        UUID userId,
        UserRole role,
        UserStatus userStatus,
        String slackId,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalDeliveryUserResponse from(
            InternalDeliveryUserResult result
    ) {
        return new InternalDeliveryUserResponse(
                result.userId(),
                result.role(),
                result.userStatus(),
                result.slackId(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}