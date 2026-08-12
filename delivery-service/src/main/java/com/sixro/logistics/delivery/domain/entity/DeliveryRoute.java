package com.sixro.logistics.delivery.domain.entity;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "delivery_schema", name = "p_delivery_route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class DeliveryRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryRouteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(nullable = false)
    private Integer routeSequence;

    @Column(nullable = false)
    private UUID originHubId;

    @Column(nullable = false)
    private UUID destHubId;

    @Column(nullable = false)
    private Long expectedDistanceM;

    @Column(nullable = false)
    private Long expectedDurationS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus routeStatus = RouteStatus.HUB_TRANSIT_WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_manager_id")
    private DeliveryManager deliveryManager;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    public static DeliveryRoute create(Delivery delivery, Integer routeSequence, UUID originHubId, UUID destHubId,
                                       Long expectedDistanceM, Long expectedDurationS) {

        DeliveryRoute deliveryRoute = new DeliveryRoute();

        deliveryRoute.delivery = delivery;
        deliveryRoute.routeSequence = routeSequence;
        deliveryRoute.originHubId = originHubId;
        deliveryRoute.destHubId = destHubId;
        deliveryRoute.expectedDistanceM = expectedDistanceM;
        deliveryRoute.expectedDurationS = expectedDurationS;
        deliveryRoute.routeStatus = RouteStatus.HUB_TRANSIT_WAITING;

        return deliveryRoute;
    }

    public void updateStatus(RouteStatus routeStatus, LocalDateTime changedAt) {
        validateStatusTransition(routeStatus);

        this.routeStatus = routeStatus;

        if (routeStatus == RouteStatus.HUB_IN_TRANSIT) {
            this.startedAt = changedAt;
        }
        if (routeStatus == RouteStatus.HUB_ARRIVED) {
            this.completedAt = changedAt;
        }
    }

    public void validateStatusTransition(RouteStatus routeStatus) {
        if (!canTransitTo(routeStatus)) {
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_ROUTE_STATUS_TRANSITION);
        }
    }

    public void cancelByDelivery() {
        if (this.routeStatus != RouteStatus.HUB_TRANSIT_WAITING) {
            throw new BaseException(DeliveryErrorCode.INVALID_DELIVERY_ROUTE_STATUS_TRANSITION);
        }

        this.routeStatus = RouteStatus.CANCELLED;
    }

    public void assignDeliveryManager(DeliveryManager deliveryManager) {
        this.deliveryManager = deliveryManager;
    }

    private boolean canTransitTo(RouteStatus routeStatus) {
        return switch (this.routeStatus) {
            case HUB_TRANSIT_WAITING -> routeStatus == RouteStatus.HUB_IN_TRANSIT || routeStatus == RouteStatus.FAILED;
            case HUB_IN_TRANSIT -> routeStatus == RouteStatus.HUB_ARRIVED || routeStatus == RouteStatus.FAILED;
            case HUB_ARRIVED, CANCELLED, FAILED -> false;
        };
    }
}
