package com.sixro.logistics.delivery.application.model;

import java.util.UUID;

// Application이 이해하는 허브 정보
public record HubInfo(
        UUID hubId,
        String hubName
) {
}
