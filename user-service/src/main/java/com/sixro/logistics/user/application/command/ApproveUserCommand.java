package com.sixro.logistics.user.application.command;

import java.util.UUID;

/**
 * 사용자 가입 승인 명령입니다.
 */
public record ApproveUserCommand(
        UUID targetUserId,
        UUID reviewerId
) {
}