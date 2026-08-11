package com.sixro.logistics.user.application.command;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

public record AdminCreateUserCommand(
        UUID requesterId,
        UserRole requesterRole,
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {
    /**
     * 비밀번호 해시가 로그에 노출되지 않도록 합니다.
     */
    @Override
    public String toString() {
        return "AdminCreateUserCommand[" +
                "requesterId=" + requesterId +
                ", requesterRole=" + requesterRole +
                ", username=" + username +
                ", encodedPassword=[PROTECTED]" +
                ", slackId=" + slackId +
                ", role=" + role +
                ", affiliationId=" + affiliationId +
                ", affiliationType=" + affiliationType +
                ']';
    }
}