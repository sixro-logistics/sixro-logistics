package com.sixro.logistics.user.application.dto;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application 계층의 공통 사용자 조회 결과입니다.
 *
 * <p>Presentation 계층에서 API별 Response DTO로 변환합니다.</p>
 */
public record UserResult(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        AffiliationType affiliationType,
        UUID affiliationId,
        UserStatus userStatus,
        LocalDateTime reviewedAt,
        UUID reviewedBy,
        String rejectedReason,
        LocalDateTime deletedAt
) {

    public static UserResult from(User user) {
        return new UserResult(
                user.getUserId(),
                user.getUsername(),
                user.getSlackId(),
                user.getRole(),
                user.getAffiliationType(),
                user.getAffiliationId(),
                user.getUserStatus(),
                user.getReviewedAt(),
                user.getReviewedBy(),
                user.getRejectedReason(),
                user.getDeletedAt()
        );
    }
}