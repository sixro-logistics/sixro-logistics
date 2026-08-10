package com.sixro.logistics.auth.infrastructure.client.request;

import com.sixro.logistics.auth.application.command.SignUpCommand;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;

import java.util.UUID;

/**
 * Auth Service에서 User Service로 전달하는 내부 사용자 생성 요청입니다.
 *
 * <p>{@code encodedPassword}에는 암호화가 완료된 비밀번호만 전달해야 합니다.</p>
 */
public record InternalCreateUserRequest(
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    /**
     * 회원가입 Command와 암호화된 비밀번호를 내부 API 요청으로 변환합니다.
     *
     * @param command         회원가입 Command
     * @param encodedPassword BCrypt로 암호화된 비밀번호
     * @return 내부 사용자 생성 요청
     */
    public static InternalCreateUserRequest from(
            SignUpCommand command,
            String encodedPassword
    ) {
        return new InternalCreateUserRequest(
                command.username(),
                encodedPassword,
                command.slackId(),
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );
    }
}