package com.sixro.logistics.delivery.application.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record DeliveryCreatedEvent(
        UUID eventId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime occurredAt,
        DeliveryCreatedData data
) {

    public record DeliveryCreatedData(
            UUID orderId,
            UUID deliveryId,
            LocalDateTime deliveryDeadline,
            String requests,
            String deliveryAddress,
            List<Product> products,
            UUID originHubId,
            UUID destHubId,
            Long totalHubRouteExpectedDurationS,
            List<Route> routes,
            DeliveryManagerWorkingHours deliveryManagerWorkingHours,
            List<DeliveryManagerInfo> deliveryManagers
    ) {
    }

    public record Product(
            UUID productId,
            Integer quantity
    ) {
    }

    public record Route(
            Integer sequence,
            UUID originHubId,
            UUID destHubId,
            Long expectedDurationS
    ) {
    }

    public record DeliveryManagerWorkingHours(
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime
    ) {
    }

    public record DeliveryManagerInfo(
            UUID deliveryManagerId,
            String managerType,
            Integer deliverySequence
    ) {
    }
}
