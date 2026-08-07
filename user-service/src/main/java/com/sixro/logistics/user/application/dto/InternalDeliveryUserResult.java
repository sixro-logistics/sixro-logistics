package com.sixro.logistics.user.application.dto;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Delivery Service에서 배송 담당자 생성 및 검증에 사용하는
 * 내부 사용자 정보입니다.
 */
public record InternalDeliveryUserResult(
        UUID userId,
        UserRole role,
        UserStatus userStatus,
        String slackId,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalDeliveryUserResult from(User user) {
        return new InternalDeliveryUserResult(
                user.getUserId(),
                user.getRole(),
                user.getUserStatus(),
                user.getSlackId(),
                user.getAffiliationId(),
                user.getAffiliationType()
        );
    }
}