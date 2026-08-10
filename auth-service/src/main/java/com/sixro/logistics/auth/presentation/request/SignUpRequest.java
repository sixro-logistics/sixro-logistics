package com.sixro.logistics.auth.presentation.request;

import com.sixro.logistics.auth.application.command.SignUpCommand;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * 회원가입 API 요청입니다.
 *
 * <p>사용자명과 비밀번호의 기본 형식을 검증하며,
 * 권한과 소속 정보의 관계는 애플리케이션 계층에서 검증합니다.</p>
 */
public record SignUpRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,10}$",
                message = "사용자명은 영문 소문자와 숫자를 사용하여 4자 이상 10자 이하로 입력해야 합니다."
        )
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,15}$",
                message = "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 포함하여 8자 이상 15자 이하로 입력해야 합니다."
        )
        String password,

        @NotBlank(message = "Slack ID는 필수입니다.")
        String slackId,

        @NotNull(message = "신청 권한은 필수입니다.")
        UserRole role,

        UUID affiliationId,

        AffiliationType affiliationType
) {

    public SignUpCommand toCommand() {
        return new SignUpCommand(
                username,
                password,
                slackId,
                role,
                affiliationId,
                affiliationType
        );
    }
}