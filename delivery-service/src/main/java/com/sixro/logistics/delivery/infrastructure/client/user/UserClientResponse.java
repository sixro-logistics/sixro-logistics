package com.sixro.logistics.delivery.infrastructure.client.user;

import java.util.UUID;

public record UserClientResponse(
        UUID userId,
        String username,
        String role,
        String userStatus,
        String slackId,
        UUID affiliationId,
        String affiliationType
) {
}
