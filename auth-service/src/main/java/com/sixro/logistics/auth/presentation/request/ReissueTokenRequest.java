package com.sixro.logistics.auth.presentation.request;

import com.sixro.logistics.auth.application.command.ReissueTokenCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 API 요청입니다.
 */
public record ReissueTokenRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {

    public ReissueTokenCommand toCommand() {
        return new ReissueTokenCommand(refreshToken);
    }
}