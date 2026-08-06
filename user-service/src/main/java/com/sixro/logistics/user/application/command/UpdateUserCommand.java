package com.sixro.logistics.user.application.command;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

/**
 * 사용자 정보 부분 수정 명령입니다.
 */
public record UpdateUserCommand(
        UUID targetUserId,
        UUID requesterId,
        UserRole requesterRole,
        String slackId,
        UserRole role,
        AffiliationType affiliationType,
        UUID affiliationId
) {
}