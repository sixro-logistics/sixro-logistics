package com.sixro.logistics.auth.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSecurityChangedMessage(
        UUID userId
) {
    public UserSecurityChangedMessage {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "사용자 보안 이벤트의 userId는 필수입니다."
            );
        }
    }
}