package com.sixro.logistics.user.presentation.response.internal;

import com.sixro.logistics.user.application.dto.InternalUserStatusResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Auth Service의 Access Token 재발급 시 사용하는
 * 최신 사용자 상태, 권한 및 소속 정보 응답 DTO입니다.
 */
public record InternalUserStatusResponse(
        UUID userId,
        UserRole role,
        UserStatus userStatus,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalUserStatusResponse from(
            InternalUserStatusResult result
    ) {
        return new InternalUserStatusResponse(
                result.userId(),
                result.role(),
                result.userStatus(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}