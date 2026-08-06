package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDetailResponse(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        AffiliationType affiliationType,
        UUID affiliationId,
        UserStatus userStatus,
        LocalDateTime reviewedAt,
        UUID reviewedBy,
        String rejectedReason
) {

    public static UserDetailResponse from(UserResult result) {
        return new UserDetailResponse(
                result.userId(),
                result.username(),
                result.slackId(),
                result.role(),
                result.affiliationType(),
                result.affiliationId(),
                result.userStatus(),
                result.reviewedAt(),
                result.reviewedBy(),
                result.rejectedReason()
        );
    }
}