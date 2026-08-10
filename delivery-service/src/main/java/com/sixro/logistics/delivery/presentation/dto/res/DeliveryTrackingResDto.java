package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryTrackingResult;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class DeliveryTrackingResDto {

    private final DeliveryInfo delivery;
    private final RouteSummary routeSummary;
    private final List<RouteInfo> routes;
    private final CompanyDeliveryProgress companyDeliveryProgress;

    public DeliveryTrackingResDto(DeliveryTrackingResult result) {
        this.delivery = new DeliveryInfo(result.getDelivery());
        this.routeSummary = new RouteSummary(result.getRouteSummary());
        this.routes = result.getRoutes().stream().map(RouteInfo::new).toList();
        this.companyDeliveryProgress = new CompanyDeliveryProgress(result.getCompanyDeliveryProgress());
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

        public DeliveryInfo(DeliveryTrackingResult.DeliveryInfo result) {
            this.deliveryId = result.getDeliveryId();
            this.orderId = result.getOrderId();
            this.deliveryStatus = result.getDeliveryStatus();
            this.originHubId = result.getOriginHubId();
            this.destHubId = result.getDestHubId();
            this.deliveryAddress = result.getDeliveryAddress();
            this.deliveryDeadline = result.getDeliveryDeadline();
            this.recipientName = result.getRecipientName();
            this.deliveryManagerId = result.getDeliveryManagerId();
        }
    }

    @Getter
    public static class RouteSummary {

        private final int totalRouteCount;
        private final long completedRouteCount;
        private final long totalExpectedDistanceM;
        private final long totalExpectedDurationS;

        public RouteSummary(DeliveryTrackingResult.RouteSummary result) {
            this.totalRouteCount = result.getTotalRouteCount();
            this.completedRouteCount = result.getCompletedRouteCount();
            this.totalExpectedDistanceM = result.getTotalExpectedDistanceM();
            this.totalExpectedDurationS = result.getTotalExpectedDurationS();
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

        public RouteInfo(DeliveryTrackingResult.RouteInfo result) {
            this.deliveryRouteId = result.getDeliveryRouteId();
            this.routeSequence = result.getRouteSequence();
            this.originHubId = result.getOriginHubId();
            this.destHubId = result.getDestHubId();
            this.expectedDistanceM = result.getExpectedDistanceM();
            this.expectedDurationS = result.getExpectedDurationS();
            this.routeStatus = result.getRouteStatus();
            this.deliveryManagerId = result.getDeliveryManagerId();
            this.startedAt = result.getStartedAt();
            this.completedAt = result.getCompletedAt();
        }
    }

    @Getter
    public static class CompanyDeliveryProgress {

        private final DeliveryStatus deliveryStatus;
        private final UUID deliveryManagerId;
        private final boolean started;
        private final boolean completed;

        public CompanyDeliveryProgress(DeliveryTrackingResult.CompanyDeliveryProgress result) {
            this.deliveryStatus = result.getDeliveryStatus();
            this.deliveryManagerId = result.getDeliveryManagerId();
            this.started = result.isStarted();
            this.completed = result.isCompleted();
        }
    }
}
