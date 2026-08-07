package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.InternalUserStatusResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

public record InternalUserStatusResponse(
        UUID userId,
        UserRole role,
        UserStatus userStatus,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalUserStatusResponse from(
            InternalUserStatusResult result
    ) {
        return new InternalUserStatusResponse(
                result.userId(),
                result.role(),
                result.userStatus(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}