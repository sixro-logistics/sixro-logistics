package com.sixro.logistics.user.application.command;

import java.util.UUID;

/**
 * 사용자 가입 거절 명령입니다.
 */
public record RejectUserCommand(
        UUID targetUserId,
        UUID reviewerId,
        String rejectedReason
) {
}