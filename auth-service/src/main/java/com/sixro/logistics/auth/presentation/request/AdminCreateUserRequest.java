package com.sixro.logistics.auth.presentation.request;

import com.sixro.logistics.auth.application.command.AdminCreateUserCommand;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdminCreateUserRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,10}$",
                message = "사용자명은 영문 소문자와 숫자로 4자 이상 10자 이하여야 합니다."
        )
        String username,

        @Schema(
                description = "사용자 비밀번호",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,15}$",
                message = "비밀번호는 공백 없이 대문자, 소문자, 숫자, 특수문자를 포함하여 8자 이상 15자 이하여야 합니다."
        )
        String password,

        @NotBlank(message = "Slack ID는 필수입니다.")
        @Size(max = 100)
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
                password,
                slackId,
                role,
                affiliationId,
                affiliationType
        );
    }

    /**
     * 평문 비밀번호가 로그에 노출되지 않도록 합니다.
     */
    @Override
    public String toString() {
        return "AdminCreateUserRequest[" +
                "username=" + username +
                ", password=[PROTECTED]" +
                ", slackId=" + slackId +
                ", role=" + role +
                ", affiliationId=" + affiliationId +
                ", affiliationType=" + affiliationType +
                ']';
    }
}