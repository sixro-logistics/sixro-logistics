package com.sixro.logistics.user.application.dto;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 내부 서비스에서 사용자의 최신 상태, 권한 및 소속을
 * 확인하기 위한 Application 계층 조회 결과입니다.
 *
 * <p>Auth Service의 토큰 재발급과 Order Service의
 * 주문 수령인 검증 등에 사용됩니다.</p>
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