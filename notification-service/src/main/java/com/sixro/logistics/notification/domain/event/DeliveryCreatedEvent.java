package com.sixro.logistics.notification.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCreatedEvent {

    private UUID eventId;
    private LocalDateTime occurredAt;
    private DeliveryCreatedData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryCreatedData {
        private UUID orderId;
        private UUID deliveryId;
        private LocalDateTime deliveryDeadline;
        private String requests;
        private String deliveryAddress;

        private List<ProductInfo> products;
        private UUID originHubId;
        private UUID destHubId;
        private Long totalHubRouteExpectedDurationS;
        private List<RouteInfo> routes;

        private DeliveryManagerWorkingHours deliveryManagerWorkingHours;
        private List<DeliveryManagerInfo> deliveryManagers;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private UUID productId;
        private Integer quantity;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInfo {
        private Integer sequence;
        private UUID originHubId;
        private UUID destHubId;
        private Long expectedDurationS;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryManagerWorkingHours {
        private String startTime;
        private String endTime;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryManagerInfo {
        private UUID deliveryManagerId;
        private String managerType;
        private Integer deliverySequence;
    }
}
