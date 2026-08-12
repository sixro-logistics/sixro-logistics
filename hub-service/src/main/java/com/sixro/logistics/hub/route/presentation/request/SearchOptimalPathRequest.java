package com.sixro.logistics.hub.route.presentation.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SearchOptimalPathRequest(
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID originHubId,

        @NotNull(message = "도착 허브 ID는 필수입니다.")
        @JsonProperty("destHubId")
        UUID destinationHubId,

        List<ProductPayload> products
) {
    public SearchOptimalPathRequest {
        if (originHubId != null && originHubId.equals(destinationHubId)) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 동일할 수 없습니다.");
        }
    }
    public record ProductPayload(UUID productId, int quantity) {}
}