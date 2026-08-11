package com.sixro.logistics.delivery.application.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record DeliveryCreationData(
        String traceId,
        UUID orderId,
        List<ProductData> products,
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
    public record ProductData(
            UUID productId,
            Integer quantity
    ) {
    }

    public record RouteData(
            Integer routeSequence,
            UUID originHubId,
            UUID destHubId,
            Long expectedDistanceM,
            Long expectedDurationS
    ) {
    }
}
