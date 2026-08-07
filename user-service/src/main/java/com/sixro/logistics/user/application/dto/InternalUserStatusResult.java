package com.sixro.logistics.user.application.dto;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 내부 서비스가 현재 사용자 권한과 상태를 확인할 때 사용하는 결과입니다.
 */
public record InternalUserStatusResult(
        UUID userId,
        UserRole role,
        UserStatus userStatus,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalUserStatusResult from(User user) {
        return new InternalUserStatusResult(
                user.getUserId(),
                user.getRole(),
                user.getUserStatus(),
                user.getAffiliationId(),
                user.getAffiliationType()
        );
    }
}