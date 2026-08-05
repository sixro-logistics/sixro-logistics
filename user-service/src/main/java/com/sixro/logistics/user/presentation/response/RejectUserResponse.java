package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 가입 거절 결과 응답입니다.
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