package com.sixro.logistics.user.domain.event;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 가입 승인 완료 시 발생하는 도메인 이벤트입니다.
 *
 * <p>Outbox에 저장된 후 Kafka를 통해 필요한 서비스에 전달됩니다.</p>
 */
public record UserApprovedEvent(
        UUID userId,
        UserStatus userStatus,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType,
        UUID reviewedBy,
        LocalDateTime reviewedAt
) {
}