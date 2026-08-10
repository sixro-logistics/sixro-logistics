package com.sixro.logistics.user.application.dto;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Auth Service의 토큰 재발급 시
 * 최신 사용자 상태, 권한 및 소속을 확인하기 위한 내부 조회 결과입니다.
 */
public record InternalUserStatusResult(
        UUID userId,
        String username,
        UserRole role,
        UserStatus userStatus,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalUserStatusResult from(User user) {
        return new InternalUserStatusResult(
                user.getUserId(),
                user.getUsername(),
                user.getRole(),
                user.getUserStatus(),
                user.getAffiliationId(),
                user.getAffiliationType()
        );
    }
}