package com.sixro.logistics.auth.presentation.response;

import com.sixro.logistics.auth.application.dto.SignUpResult;
import com.sixro.logistics.auth.domain.model.UserStatus;

import java.util.UUID;

/**
 * 회원가입 성공 응답 DTO입니다.
 */
public record SignUpResponse(
        UUID userId,
        String username,
        UserStatus userStatus
) {

    /**
     * 애플리케이션 계층의 회원가입 처리 결과를 응답 DTO로 변환합니다.
     *
     * @param result 회원가입 처리 결과
     * @return 회원가입 응답
     */
    public static SignUpResponse from(SignUpResult result) {
        return new SignUpResponse(
                result.userId(),
                result.username(),
                result.userStatus()
        );
    }
}