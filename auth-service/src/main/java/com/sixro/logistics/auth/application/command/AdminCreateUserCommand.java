package com.sixro.logistics.auth.application.command;

import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;

import java.util.UUID;

public record AdminCreateUserCommand(
        UUID requesterId,
        UserRole requesterRole,
        String username,
        String rawPassword,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {
    /**
     * 비밀번호가 로그에 노출되지 않도록 기본 toString을 재정의합니다.
     */
    @Override
    public String toString() {
        return "AdminCreateUserCommand[" +
                "requesterId=" + requesterId +
                ", requesterRole=" + requesterRole +
                ", username=" + username +
                ", rawPassword=[PROTECTED]" +
                ", slackId=" + slackId +
                ", role=" + role +
                ", affiliationId=" + affiliationId +
                ", affiliationType=" + affiliationType +
                ']';
    }
}