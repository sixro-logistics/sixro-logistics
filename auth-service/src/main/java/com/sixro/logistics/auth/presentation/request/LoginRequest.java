package com.sixro.logistics.auth.presentation.request;

import com.sixro.logistics.auth.application.command.LoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 API 요청입니다.
 *
 * <p>형식 검증 후 애플리케이션 계층의 {@link LoginCommand}로 변환합니다.</p>
 */
public record LoginRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
    // 검증된 로그인 요청을 애플리케이션 Command로 변환합니다.
    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}