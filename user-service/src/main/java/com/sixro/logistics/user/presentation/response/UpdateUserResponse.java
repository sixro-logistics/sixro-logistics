package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

/**
 * 사용자 정보 수정 결과를 반환하는 응답 DTO입니다.
 */
public record UpdateUserResponse(
        UUID userId,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static UpdateUserResponse from(UserResult result) {
        return new UpdateUserResponse(
                result.userId(),
                result.slackId(),
                result.role(),
                result.affiliationId(),
                result.affiliationType()
        );
    }
}