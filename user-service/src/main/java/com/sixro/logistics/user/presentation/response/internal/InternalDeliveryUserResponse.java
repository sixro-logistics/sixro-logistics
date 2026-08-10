package com.sixro.logistics.user.presentation.response.internal;

import com.sixro.logistics.user.application.dto.InternalDeliveryUserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * Delivery Service의 배송 담당자 검증 및
 * 수령인 정보 조회에 사용하는 내부 사용자 정보 응답 DTO입니다.
 */
public record InternalDeliveryUserResponse(
        UUID userId,
        String username,
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
                result.username(),
                result.role(),
                result.userStatus(),
                result.slackId(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}