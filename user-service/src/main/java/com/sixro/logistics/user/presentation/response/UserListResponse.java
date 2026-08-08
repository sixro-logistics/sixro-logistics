package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * MASTER_ADMIN이 사용자 목록을 조회할 때 사용하는 응답 DTO입니다.
 */
public record UserListResponse(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
        UserStatus userStatus
) {

    public static UserListResponse from(UserResult result) {
        return new UserListResponse(
                result.userId(),
                result.username(),
                result.slackId(),
                result.role(),
                result.affiliationId(),
                result.affiliationType(),
                result.userStatus()
        );
    }
}