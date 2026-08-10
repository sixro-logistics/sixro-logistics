package com.sixro.logistics.user.presentation.request.internal;

import com.sixro.logistics.user.application.command.CreateUserCommand;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Auth Service가 User Service의 내부 사용자 생성 API를 호출할 때 사용하는 요청 DTO입니다.
 */
public record InternalCreateUserRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 4, max = 50, message = "사용자명은 4자 이상 50자 이하여야 합니다.")
        String username,

        @NotBlank(message = "암호화된 비밀번호는 필수입니다.")
        String encodedPassword,

        @NotBlank(message = "Slack ID는 필수입니다.")
        @Size(max = 100, message = "Slack ID는 100자 이하여야 합니다.")
        String slackId,

        @NotNull(message = "사용자 권한은 필수입니다.")
        UserRole role,

        UUID affiliationId,

        AffiliationType affiliationType

) {

    public CreateUserCommand toCommand() {
        return new CreateUserCommand(
                username,
                encodedPassword,
                slackId,
                role,
                affiliationId,
                affiliationType
        );
    }
}