package com.sixro.logistics.order.application.model;

import com.sixro.logistics.order.common.model.AffiliationType;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.common.model.UserStatus;

import java.util.UUID;

public record UserInfo(
        UUID userId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
        UserStatus userStatus
) {
}
