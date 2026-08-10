package com.sixro.logistics.user.domain.event;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.util.UUID;

/**
 * 사용자 생성 완료 시 발생하는 도메인 이벤트입니다.
 */
public record UserCreatedEvent(
        UUID userId,
        UserStatus userStatus,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {
}