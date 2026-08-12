package com.sixro.logistics.auth.infrastructure.client.response;

import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;
import java.util.UUID;

public record InternalUserStatusResponse(
        UUID userId,
        String username,
        UserRole role,
        UserStatus userStatus,
        UUID affiliationId,
        AffiliationType affiliationType
) {
}