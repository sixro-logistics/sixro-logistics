package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 가입 승인 결과 응답입니다.
 */
public record ApproveUserResponse(
        UUID userId,
        UserStatus userStatus,
        LocalDateTime reviewedAt,
        UUID reviewedBy
) {

    public static ApproveUserResponse from(UserResult result) {
        return new ApproveUserResponse(
                result.userId(),
                result.userStatus(),
                result.reviewedAt(),
                result.reviewedBy()
        );
    }
}