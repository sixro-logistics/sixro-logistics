package com.sixro.logistics.user.application.command;

import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

/**
 * 사용자 비활성화 명령입니다.
 */
public record DeactivateUserCommand(
        UUID targetUserId,
        UUID requesterId,
        UserRole requesterRole
) {
}