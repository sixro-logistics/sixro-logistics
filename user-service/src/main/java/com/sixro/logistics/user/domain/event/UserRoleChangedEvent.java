package com.sixro.logistics.user.domain.event;

import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

/**
 * 사용자 역할 변경 완료 시 발행하는 도메인 이벤트입니다.
 */
public record UserRoleChangedEvent(
        UUID userId,
        UserRole previousRole,
        UserRole newRole
) {
}