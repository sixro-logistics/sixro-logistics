package com.sixro.logistics.delivery.application.model;

import java.util.UUID;

// Application이 이해하는 사용자 정보
public record UserInfo(
        UUID userId,
        String username,
        String role,
        String userStatus,
        String slackId,
        UUID affiliationId,
        String affiliationType
) {
}
