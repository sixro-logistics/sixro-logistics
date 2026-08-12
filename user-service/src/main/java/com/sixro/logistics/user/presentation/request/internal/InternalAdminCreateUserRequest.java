package com.sixro.logistics.user.presentation.request.internal;

import com.sixro.logistics.user.application.command.AdminCreateUserCommand;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InternalAdminCreateUserRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,10}$",
                message = "사용자명은 영문 소문자와 숫자로 4자 이상 10자 이하여야 합니다."
        )
        String username,

        @NotBlank(message = "비밀번호 해시는 필수입니다.")
        @Size(
                max = 255,
                message = "비밀번호 해시는 255자 이하여야 합니다."
        )
        String encodedPassword,

        @NotBlank(message = "Slack ID는 필수입니다.")
        @Size(
                max = 100,
                message = "Slack ID는 100자 이하여야 합니다."
        )
        String slackId,

        @NotNull(message = "사용자 권한은 필수입니다.")
        UserRole role,

        UUID affiliationId,

        AffiliationType affiliationType
) {

    public AdminCreateUserCommand toCommand(
            UUID requesterId,
            UserRole requesterRole
    ) {
        return new AdminCreateUserCommand(
                requesterId,
                requesterRole,
                username,
                encodedPassword,
                slackId,
                role,
                affiliationId,
                affiliationType
        );
    }

    /**
     * 비밀번호 해시가 로그에 노출되지 않도록 합니다.
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