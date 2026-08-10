package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MASTER_ADMIN의 사용자 가입 승인 처리 결과를 반환하는 응답 DTO입니다.
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