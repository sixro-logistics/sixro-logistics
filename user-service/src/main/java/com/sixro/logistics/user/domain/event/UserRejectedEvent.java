package com.sixro.logistics.user.domain.event;

import com.sixro.logistics.user.domain.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 가입 거절 완료 시 발행하는 도메인 이벤트입니다.
 */
public record UserRejectedEvent(
        UUID userId,
        UserStatus userStatus,
        String rejectedReason,
        UUID reviewedBy,
        LocalDateTime reviewedAt
) {
}