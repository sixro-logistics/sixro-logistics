package com.sixro.logistics.hub.application.command;

import java.util.UUID;

public record UserContext(
        UUID userId,
        String role,
        String affiliationType,
        UUID affiliationId
) {
    public boolean isMasterAdmin() {
        return "MASTER_ADMIN".equals(role);
    }

    public boolean isHubAdmin() {
        return "HUB_ADMIN".equals(role);
    }
}