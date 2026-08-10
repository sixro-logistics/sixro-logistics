package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class DeliveryTrackingResult {

    private final DeliveryInfo delivery;
    private final RouteSummary routeSummary;
    private final List<RouteInfo> routes;
    private final CompanyDeliveryProgress companyDeliveryProgress;

    public DeliveryTrackingResult(Delivery delivery, List<DeliveryRoute> deliveryRoutes) {
        this.delivery = new DeliveryInfo(delivery);
        this.routeSummary = new RouteSummary(deliveryRoutes);
        this.routes = deliveryRoutes.stream().map(RouteInfo::new).toList();
        this.companyDeliveryProgress = new CompanyDeliveryProgress(delivery);
    }

    @Getter
    public static class DeliveryInfo {

        private final UUID deliveryId;
        private final UUID orderId;
        private final DeliveryStatus deliveryStatus;
        private final UUID originHubId;
        private final UUID destHubId;
        private final String deliveryAddress;
        private final LocalDateTime deliveryDeadline;
        private final String recipientName;
        private final UUID deliveryManagerId;

        public DeliveryInfo(Delivery delivery) {
            this.deliveryId = delivery.getDeliveryId();
            this.orderId = delivery.getOrderId();
            this.deliveryStatus = delivery.getDeliveryStatus();
            this.originHubId = delivery.getOriginHubId();
            this.destHubId = delivery.getDestHubId();
            this.deliveryAddress = delivery.getDeliveryAddress();
            this.deliveryDeadline = delivery.getDeliveryDeadline();
            this.recipientName = delivery.getRecipientName();
            this.deliveryManagerId = delivery.getDeliveryManager() == null ? null : delivery.getDeliveryManager().getDeliveryManagerId();
        }
    }

    @Getter
    public static class RouteSummary {

        private final int totalRouteCount;
        private final long completedRouteCount;
        private final long totalExpectedDistanceM;
        private final long totalExpectedDurationS;

        public RouteSummary(List<DeliveryRoute> deliveryRoutes) {
            this.totalRouteCount = deliveryRoutes.size();
            this.completedRouteCount = deliveryRoutes.stream()
                    .filter(deliveryRoute -> deliveryRoute.getRouteStatus() == RouteStatus.HUB_ARRIVED).count();
            this.totalExpectedDistanceM = deliveryRoutes.stream().mapToLong(DeliveryRoute::getExpectedDistanceM).sum();
            this.totalExpectedDurationS = deliveryRoutes.stream().mapToLong(DeliveryRoute::getExpectedDurationS).sum();
        }
    }

    @Getter
    public static class RouteInfo {

        private final UUID deliveryRouteId;
        private final Integer routeSequence;
        private final UUID originHubId;
        private final UUID destHubId;
        private final Long expectedDistanceM;
        private final Long expectedDurationS;
        private final RouteStatus routeStatus;
        private final UUID deliveryManagerId;
        private final LocalDateTime startedAt;
        private final LocalDateTime completedAt;

        public RouteInfo(DeliveryRoute deliveryRoute) {
            this.deliveryRouteId = deliveryRoute.getDeliveryRouteId();
            this.routeSequence = deliveryRoute.getRouteSequence();
            this.originHubId = deliveryRoute.getOriginHubId();
            this.destHubId = deliveryRoute.getDestHubId();
            this.expectedDistanceM = deliveryRoute.getExpectedDistanceM();
            this.expectedDurationS = deliveryRoute.getExpectedDurationS();
            this.routeStatus = deliveryRoute.getRouteStatus();
            this.deliveryManagerId = deliveryRoute.getDeliveryManager() == null
                    ? null : deliveryRoute.getDeliveryManager().getDeliveryManagerId();
            this.startedAt = deliveryRoute.getStartedAt();
            this.completedAt = deliveryRoute.getCompletedAt();
        }
    }

    @Getter
    public static class CompanyDeliveryProgress {

        private final DeliveryStatus deliveryStatus;
        private final UUID deliveryManagerId;
        private final boolean started;
        private final boolean completed;

        public CompanyDeliveryProgress(Delivery delivery) {
            this.deliveryStatus = delivery.getDeliveryStatus();
            this.deliveryManagerId = delivery.getDeliveryManager() == null
                    ? null : delivery.getDeliveryManager().getDeliveryManagerId();
            this.started = delivery.getDeliveryStatus() == DeliveryStatus.COMPANY_DELIVERY_IN_PROGRESS
                    || delivery.getDeliveryStatus() == DeliveryStatus.DELIVERED;
            this.completed = delivery.getDeliveryStatus() == DeliveryStatus.DELIVERED;
        }
    }
}
