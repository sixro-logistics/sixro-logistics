package com.sixro.logistics.user.presentation.response.internal;

import com.sixro.logistics.user.application.dto.InternalDeliveryUserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Delivery Service가 배송 담당자 생성 및 검증에 사용하는
 * 내부 사용자 정보 응답 DTO입니다.
 */
public record InternalDeliveryUserResponse(
        UUID userId,
        UserRole role,
        UserStatus userStatus,
        String slackId,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalDeliveryUserResponse from(
            InternalDeliveryUserResult result
    ) {
        return new InternalDeliveryUserResponse(
                result.userId(),
                result.role(),
                result.userStatus(),
                result.slackId(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}