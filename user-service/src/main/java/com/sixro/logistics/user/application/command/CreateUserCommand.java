package com.sixro.logistics.user.application.command;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;

import java.util.UUID;

/**
 * 신규 사용자 생성에 필요한 Application 계층 명령 객체입니다.
 */
public record CreateUserCommand(
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {
}
