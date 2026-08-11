package com.sixro.logistics.user.presentation.response.internal;

import com.sixro.logistics.user.application.dto.InternalUserStatusResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 내부 서비스의 사용자 상태 검증에 사용하는 응답 DTO입니다.
 *
 * <p>Auth Service의 토큰 재발급과 Order Service의
 * 주문 수령인 검증 등에 사용됩니다.</p>
 */
public record InternalUserStatusResponse(
        UUID userId,
        String username,
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
                result.username(),
                result.role(),
                result.userStatus(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}