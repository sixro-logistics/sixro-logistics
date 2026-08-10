package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 로그인한 사용자의 기본 정보를 반환하는 응답 DTO입니다.
 */
public record MyUserResponse(
        UUID userId,
        String username,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
        UserStatus userStatus
) {

    public static MyUserResponse from(UserResult result) {
        return new MyUserResponse(
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