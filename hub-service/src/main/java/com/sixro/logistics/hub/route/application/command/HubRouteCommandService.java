package com.sixro.logistics.hub.route.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.application.port.ExternalRoutePort;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import com.sixro.logistics.hub.route.application.port.RouteSearchCondition;
import com.sixro.logistics.hub.route.application.port.RouteSnapshotResult;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.RouteCost;
import com.sixro.logistics.hub.route.domain.policy.RouteCostCalculationPolicy;
import com.sixro.logistics.hub.route.domain.repository.HubRouteCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouteCommandService {

    private final HubRouteCommandRepository hubRouteCommandRepository; // 단순 조회를 위해 유지
    private final HubRouteStore hubRouteStore; // 트랜잭션 전담 스토어
    private final ExternalRoutePort externalRoutePort;
    private final HubInfoPort hubInfoPort;
    private final RouteCostCalculationPolicy routeCostCalculationPolicy;

    public UUID createRoute(CreateHubRouteCommand command) {
        if (hubRouteCommandRepository.existsByOriginHubIdAndDestinationHubId(command.originHubId(), command.destinationHubId())) {
            throw new BaseException(HubRouteErrorCode.DUPLICATE_HUB_ROUTE);
        }

        HubInfoPort.HubCoordinate origin = hubInfoPort.getHubCoordinate(command.originHubId());
        HubInfoPort.HubCoordinate destination = hubInfoPort.getHubCoordinate(command.destinationHubId());

        RouteSearchCondition condition = RouteSearchCondition.builder()
                .startX(origin.longitude()).startY(origin.latitude())
                .endX(destination.longitude()).endY(destination.latitude())
                .totalValue(1)
                .trafficInfo("N")
                .build();

        // HTTP 통신 구간
        RouteSnapshotResult snapshot = externalRoutePort.getRouteSnapshot(condition);
        int calculatedBaseCost = routeCostCalculationPolicy.calculateCost(snapshot.distance());

        RouteCost routeCost = new RouteCost(calculatedBaseCost, snapshot.tollFee());
        HubRoute hubRoute = HubRoute.builder()
                .originHubId(command.originHubId())
                .destinationHubId(command.destinationHubId())
                .distance(snapshot.distance())
                .duration(snapshot.duration())
                .routeCost(routeCost)
                .routePath(snapshot.routePath())
                .build();

        return hubRouteStore.save(hubRoute);
    }

    public UUID syncRoute(UUID routeId, SyncHubRouteCommand command) {
        // 단건 조회
        HubRoute hubRoute = hubRouteCommandRepository.findById(routeId)
                .orElseThrow(() -> new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));

        HubInfoPort.HubCoordinate origin = hubInfoPort.getHubCoordinate(hubRoute.getOriginHubId());
        HubInfoPort.HubCoordinate destination = hubInfoPort.getHubCoordinate(hubRoute.getDestinationHubId());

        RouteSearchCondition condition = RouteSearchCondition.builder()
                .startX(origin.longitude()).startY(origin.latitude())
                .endX(destination.longitude()).endY(destination.latitude())
                .totalValue(command.totalValue())
                .trafficInfo(command.trafficInfo())
                .build();

        // HTTP 통신 구간
        RouteSnapshotResult snapshot = externalRoutePort.getRouteSnapshot(condition);
        int recalculatedBaseCost = routeCostCalculationPolicy.calculateCost(snapshot.distance());
        RouteCost recalculatedCost = new RouteCost(recalculatedBaseCost, snapshot.tollFee());

        LineString newRoutePath = snapshot.routePath() != null ? snapshot.routePath() : hubRoute.getRoutePath();

        boolean isChanged = isRouteDataChanged(hubRoute, snapshot, recalculatedCost);

        // 변경 사항 DB 갱신
        hubRouteStore.updateCostAndPath(routeId, snapshot.distance(), snapshot.duration(), recalculatedCost, newRoutePath);

        if (isChanged) {
            log.info("[HubRoute 단건 동기화] 데이터 변동 감지 및 이벤트 발행 - RouteID: {}", routeId);
            // TODO: HubRouteChangedEvent 발행
        }

        return hubRoute.getId();
    }

    public boolean syncRouteForBatch(HubRoute hubRoute, HubInfoPort.HubCoordinate origin, HubInfoPort.HubCoordinate destination, int totalValue) {

        RouteSearchCondition condition = RouteSearchCondition.builder()
                .startX(origin.longitude()).startY(origin.latitude())
                .endX(destination.longitude()).endY(destination.latitude())
                .totalValue(totalValue)
                .trafficInfo("N")
                .build();

        // HTTP 통신 구간
        RouteSnapshotResult snapshot = externalRoutePort.getRouteSnapshot(condition);
        int recalculatedBaseCost = routeCostCalculationPolicy.calculateCost(snapshot.distance());
        RouteCost recalculatedCost = new RouteCost(recalculatedBaseCost, snapshot.tollFee());

        LineString newRoutePath = snapshot.routePath() != null ? snapshot.routePath() : hubRoute.getRoutePath();

        boolean isChanged = isRouteDataChanged(hubRoute, snapshot, recalculatedCost);

        if (isChanged) {
            hubRoute.update(snapshot.distance(), snapshot.duration(), recalculatedCost, newRoutePath);
            hubRouteStore.save(hubRoute);
        }

        return isChanged;
    }

    public UUID updateRoute(UUID routeId, UpdateHubRouteCommand command) {
        HubRoute hubRoute = hubRouteCommandRepository.findById(routeId)
                .orElseThrow(() -> new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));

        RouteCost updatedCost = new RouteCost(command.baseCost(), hubRoute.getRouteCost().getTollFee());
        return hubRouteStore.updateCostAndPath(routeId, hubRoute.getDistance(), hubRoute.getDuration(), updatedCost, hubRoute.getRoutePath());
    }

    public void deleteRoute(UUID routeId, UUID deletedBy) {
        hubRouteStore.delete(routeId, deletedBy);
        // TODO: 캐시 일괄 무효화 및 이벤트 발행
    }

    private boolean isRouteDataChanged(HubRoute hubRoute, RouteSnapshotResult snapshot, RouteCost newCost) {
        return hubRoute.getDistance() != snapshot.distance() ||
                hubRoute.getDuration() != snapshot.duration() ||
                !hubRoute.getRouteCost().equals(newCost);
    }
}