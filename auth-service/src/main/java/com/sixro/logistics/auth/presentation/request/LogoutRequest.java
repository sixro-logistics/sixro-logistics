package com.sixro.logistics.auth.presentation.request;

import com.sixro.logistics.auth.application.command.LogoutCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 API 요청입니다.
 *
 * <p>Access Token은 Authorization 헤더에서 받고,
 * Refresh Token은 요청 본문에서 받습니다.</p>
 */
public record LogoutRequest(

        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {

    public LogoutCommand toCommand(String accessToken) {
        return new LogoutCommand(accessToken, refreshToken);
    }
}