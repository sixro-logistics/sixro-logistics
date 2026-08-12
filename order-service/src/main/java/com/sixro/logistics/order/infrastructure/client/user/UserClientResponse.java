package com.sixro.logistics.order.infrastructure.client.user;

import com.sixro.logistics.order.common.model.AffiliationType;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.common.model.UserStatus;

import java.util.UUID;

public record UserClientResponse(
        UUID userId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
        UserStatus userStatus
) {
}
