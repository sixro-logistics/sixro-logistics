package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MASTER_ADMIN이 특정 사용자의 상세 정보와
 * 가입 심사 정보를 조회할 때 사용하는 응답 DTO입니다.
 */
public record UserDetailResponse(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
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
                result.affiliationId(),
                result.affiliationType(),
                result.userStatus(),
                result.reviewedAt(),
                result.reviewedBy(),
                result.rejectedReason()
        );
    }
}