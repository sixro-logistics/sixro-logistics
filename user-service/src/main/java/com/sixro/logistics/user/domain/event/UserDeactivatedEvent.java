package com.sixro.logistics.user.domain.event;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 비활성화 완료 시 발생하는 도메인 이벤트입니다.
 *
 * <p>인증 상태 및 사용자와 연관된 데이터의
 * 비활성화 처리를 위해 사용됩니다.</p>
 */
public record UserDeactivatedEvent(
        UUID userId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
        UUID deletedBy,
        LocalDateTime deletedAt
) {
}