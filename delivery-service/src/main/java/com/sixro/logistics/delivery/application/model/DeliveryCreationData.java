package com.sixro.logistics.delivery.application.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record DeliveryCreationData(
        UUID orderId,
        Set<UUID> supplierCompanyIds,
        UUID recipientCompanyId,
        UUID originHubId,
        UUID destHubId,
        String deliveryAddress,
        LocalDateTime deliveryDeadline,
        String requests,
        String recipientName,
        String recipientSlackId,
        List<RouteData> routes
) {
    public record RouteData(
            Integer routeSequence,
            UUID originHubId,
            UUID destHubId,
            Long expectedDistanceM,
            Long expectedDurationS
    ) {
    }
}
