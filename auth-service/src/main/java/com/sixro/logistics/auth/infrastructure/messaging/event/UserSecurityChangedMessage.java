package com.sixro.logistics.auth.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSecurityChangedMessage(
        UUID userId
) {
}