package com.sixro.logistics.hub.route.domain.model;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.LineString;

import java.util.UUID;

@Entity
@Getter
@Table(schema = "hub_schema", name = "p_hub_route")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class HubRoute extends BaseEntity {

    private static final int MAX_DISTANCE_METERS = 1_000_000; // 1,000km (국내 기준 최대 허용치)
    private static final int MAX_DURATION_SECONDS = 86_400;   // 24시간

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hub_route_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "origin_hub_id", nullable = false, columnDefinition = "uuid")
    private UUID originHubId;

    @Column(name = "destination_hub_id", nullable = false, columnDefinition = "uuid")
    private UUID destinationHubId;

    @Column(name = "distance", nullable = false)
    private int distance;

    @Column(name = "duration", nullable = false)
    private int duration;

    @Embedded
    private RouteCost routeCost;

    @Column(name = "route_path", nullable = false, columnDefinition = "geometry(LineString, 4326)")
    private LineString routePath;

    @Builder
    public HubRoute(UUID originHubId, UUID destinationHubId, int distance, int duration, RouteCost routeCost, LineString routePath) {
        validateHubs(originHubId, destinationHubId);
        validateDistance(distance);
        validateDuration(duration);
        validateRoutePath(routePath);

        this.originHubId = originHubId;
        this.destinationHubId = destinationHubId;
        this.distance = distance;
        this.duration = duration;
        this.routeCost = routeCost;
        this.routePath = routePath;
    }

    public void update(int distance, int duration, RouteCost routeCost, LineString routePath) {
        validateDistance(distance);
        validateDuration(duration);
        validateRoutePath(routePath);

        this.distance = distance;
        this.duration = duration;
        this.routeCost = routeCost;
        this.routePath = routePath;
    }

    private void validateHubs(UUID origin, UUID destination) {
        if (origin.equals(destination)) {
            throw new BaseException(HubRouteErrorCode.SAME_ORIGIN_AND_DESTINATION);
        }
    }

    private void validateDistance(int distance) {
        if (distance <= 0 || distance > MAX_DISTANCE_METERS) {
            throw new BaseException(HubRouteErrorCode.INVALID_DISTANCE);
        }
    }

    private void validateDuration(int duration) {
        if (duration <= 0 || duration > MAX_DURATION_SECONDS) {
            throw new BaseException(HubRouteErrorCode.INVALID_DURATION);
        }
    }

    private void validateRoutePath(LineString routePath) {
        if (routePath == null || routePath.isEmpty() || routePath.getNumPoints() < 2) {
            throw new BaseException(HubRouteErrorCode.INVALID_ROUTE_PATH);
        }
    }
}