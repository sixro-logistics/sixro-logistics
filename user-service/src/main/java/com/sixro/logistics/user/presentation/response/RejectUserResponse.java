package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MASTER_ADMIN의 사용자 가입 거절 처리 결과를 반환하는 응답 DTO입니다.
 */
public record RejectUserResponse(
        UUID userId,
        UserStatus userStatus,
        LocalDateTime reviewedAt,
        UUID reviewedBy,
        String rejectedReason
) {

    public static RejectUserResponse from(UserResult result) {
        return new RejectUserResponse(
                result.userId(),
                result.userStatus(),
                result.reviewedAt(),
                result.reviewedBy(),
                result.rejectedReason()
        );
    }
}