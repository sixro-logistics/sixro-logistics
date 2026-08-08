package com.sixro.logistics.user.presentation.response;

import com.sixro.logistics.user.application.dto.UserResult;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 비활성화 처리 결과를 반환하는 응답 DTO입니다.
 */
public record DeactivateUserResponse(
        UUID userId,
        LocalDateTime deletedAt
) {

    public static DeactivateUserResponse from(UserResult result) {
        return new DeactivateUserResponse(
                result.userId(),
                result.deletedAt()
        );
    }
}