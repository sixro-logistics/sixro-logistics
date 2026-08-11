package com.sixro.logistics.auth.infrastructure.client.request;

import com.sixro.logistics.auth.application.command.AdminCreateUserCommand;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;

import java.util.UUID;

public record InternalAdminCreateUserRequest(
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public static InternalAdminCreateUserRequest from(
            AdminCreateUserCommand command,
            String encodedPassword
    ) {
        return new InternalAdminCreateUserRequest(
                command.username(),
                encodedPassword,
                command.slackId(),
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );
    }

    /**
     * 암호화된 비밀번호가 로그에 노출되지 않도록 합니다.
     */
    @Override
    public String toString() {
        return "InternalAdminCreateUserRequest[" +
                "username=" + username +
                ", encodedPassword=[PROTECTED]" +
                ", slackId=" + slackId +
                ", role=" + role +
                ", affiliationId=" + affiliationId +
                ", affiliationType=" + affiliationType +
                ']';
    }
}