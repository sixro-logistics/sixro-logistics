package com.sixro.logistics.hub.route.application.usecase;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import com.sixro.logistics.hub.route.application.port.RouteMetricsPort;
import com.sixro.logistics.hub.route.application.query.OptimalPathResult;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.PathSearchType;
import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;
import com.sixro.logistics.hub.route.domain.policy.RouteCostCalculationPolicy;
import com.sixro.logistics.hub.route.domain.repository.HubRouteQueryRepository;
import com.sixro.logistics.hub.route.domain.service.PathFinder;
import com.sixro.logistics.hub.route.domain.strategy.RoutingWeightStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindOptimalPathService {

    private static final int DEFAULT_BASE_TRANSFER_TIME_SECONDS = 7200; // 2시간
    private static final int DEFAULT_MIN_CAPACITY = 10000;

    private final HubRouteQueryRepository hubRouteQueryRepository;
    private final PathFinder pathFinder;
    private final List<RoutingWeightStrategy> weightStrategies;
    private final RouteCostCalculationPolicy costCalculationPolicy;
    private final HubInfoPort hubInfoPort;
    private final RouteMetricsPort routeMetricsPort;

    public OptimalPathResult searchOptimalPath(UUID originHubId, UUID destinationHubId, PathSearchType searchType) {
        // 기초 데이터 로드
        List<RouteNetworkEdge> allRoutes = hubRouteQueryRepository.findAllOperatingRouteEdges();
        Set<UUID> closedHubIds = hubInfoPort.findClosedHubIds();

        // 출도착 허브가 CLOSED 가 아닌지 유효성 검증
        validateOriginAndDestination(originHubId, destinationHubId, closedHubIds);

        // 운영 노선망 필터링 (Operating: != CLOSED && isDeleted = false)
        List<RouteNetworkEdge> validRoutes = filterValidRoutes(allRoutes, closedHubIds);

        // 운영 노선망(Route Network) 내 운영 노선(Route)의 출/도착 허브 ID 추출
        List<UUID> validHubIds = extractValidHubIds(validRoutes);

        // Redis에서 허브별 실시간 물동량 조회, 없다면 DB 에서 조회
        Map<UUID, Integer> currentVolumes = getVolumesWithSelfHealing(validHubIds);

        // 다익스트라 연산용 실시간 혼잡률 메트릭 생성
        Map<UUID, HubTransferMetric> transferMetricsMap = buildTransferMetrics(validHubIds, currentVolumes);

        // 탐색 전략 선택
        RoutingWeightStrategy strategy = getRoutingStrategy(searchType);

        // 다익스트라 실행
        List<RouteNetworkEdge> optimalPath = pathFinder.findOptimalPath(
                validRoutes, originHubId, destinationHubId, strategy, transferMetricsMap
        );

        // 결과를 응답 DTO에 매핑
        return createOptimalPathResult(originHubId, destinationHubId, optimalPath, transferMetricsMap);
    }

    // --- Private Helper Methods ---

    private void validateOriginAndDestination(UUID originHubId, UUID destinationHubId, Set<UUID> closedHubIds) {
        if (closedHubIds.contains(originHubId)) throw new BaseException(HubRouteErrorCode.ORIGIN_HUB_CLOSED);
        if (closedHubIds.contains(destinationHubId)) throw new BaseException(HubRouteErrorCode.DESTINATION_HUB_CLOSED);
    }

    private List<RouteNetworkEdge> filterValidRoutes(List<RouteNetworkEdge> allRoutes, Set<UUID> closedHubIds) {
        return allRoutes.stream()
                .filter(route -> !closedHubIds.contains(route.originHubId()) &&
                        !closedHubIds.contains(route.destinationHubId()))
                .toList();
    }

    private List<UUID> extractValidHubIds(List<RouteNetworkEdge> validRoutes) {
        return validRoutes.stream()
                .flatMap(route -> Stream.of(route.originHubId(), route.destinationHubId()))
                .distinct()
                .toList();
    }

    /**
     * Redis에서 볼륨을 조회하고, 누락된 데이터가 있으면 DB 스냅샷으로 복구 (Fallback)
     */
    private Map<UUID, Integer> getVolumesWithSelfHealing(List<UUID> validHubIds) {
        // [Redis] 허브별 실시간 물동량 조회
        Map<UUID, Integer> currentVolumes = routeMetricsPort.getCurrentVolumes(validHubIds);

        // 방어 로직
        List<UUID> missingHubIds = validHubIds.stream()
                .filter(id -> !currentVolumes.containsKey(id))
                .toList();

        if (!missingHubIds.isEmpty()) {
            // [DB] (p_hub_metrics) 스냅샷 조회
            Map<UUID, Integer> dbVolumes = hubInfoPort.getHubVolumeSnapshots(missingHubIds);

            for (UUID missingId : missingHubIds) {
                // DB에도 값이 없다면 장애가 아닌 신규 허브이므로 0으로 초기화
                int fallbackVolume = dbVolumes.getOrDefault(missingId, 0);
                currentVolumes.put(missingId, fallbackVolume); // 메모리 즉시 보충
                routeMetricsPort.setVolume(missingId, fallbackVolume); // Redis 자가 복구
            }
            log.warn("[Redis Fallback 동작] 실시간 물동량 데이터 누락 감지. DB 스냅샷으로 {}개 허브 자가 복구 완료", missingHubIds.size());
        }

        return currentVolumes;
    }

    private Map<UUID, HubTransferMetric> buildTransferMetrics(List<UUID> validHubIds, Map<UUID, Integer> currentVolumes) {
        // [DB] (p_hub) 허브별 최대 처리 용량(CAPA) 조회 목록 조회
        Map<UUID, Integer> hubCapacities = hubInfoPort.getHubCapacities(validHubIds);

        // 다익스트라 연산용 혼잡률 메트릭 Map 생성
        return validHubIds.stream()
                .collect(Collectors.toMap(
                        hubId -> hubId,
                        hubId -> {
                            int volume = currentVolumes.getOrDefault(hubId, 0);
                            int capa = hubCapacities.getOrDefault(hubId, DEFAULT_MIN_CAPACITY);
                            return HubTransferMetric.of(hubId, DEFAULT_BASE_TRANSFER_TIME_SECONDS, volume, capa);
                        }
                ));
    }

    private RoutingWeightStrategy getRoutingStrategy(PathSearchType searchType) {
        return weightStrategies.stream()
                .filter(s -> s.getSupportedType() == searchType)
                .findFirst()
                .orElseThrow(() -> new BaseException(HubRouteErrorCode.UNSUPPORTED_PATH_SEARCH_TYPE));
    }

    private OptimalPathResult createOptimalPathResult(
            UUID originHubId, UUID destinationHubId,
            List<RouteNetworkEdge> optimalPath,
            Map<UUID, HubTransferMetric> transferMetricsMap)
    {
        AtomicInteger sequence = new AtomicInteger(1);

        List<OptimalPathResult.RouteSegment> segments = optimalPath.stream()
                .map(route -> new OptimalPathResult.RouteSegment(
                        route.routeId(),
                        sequence.getAndIncrement(),
                        route.originHubId(),
                        route.destinationHubId(),
                        route.distance(),
                        route.duration(), // 간선은 도착시간이니 환적시간 미포함, TODO: dto에 구간별 환적시간 필드 별도 추가
                        costCalculationPolicy.calculatePathCost(route) // 구간별 비용 합산
                )).toList();

        int totalCost = optimalPath.stream().mapToInt(costCalculationPolicy::calculatePathCost).sum(); // 총 비용 합산
        int totalDistance = optimalPath.stream().mapToInt(RouteNetworkEdge::distance).sum();
        int totalDuration = calculateTotalDuration(optimalPath, transferMetricsMap); // 총 시간 합산 (총 환적시간 포함)

//        int totalDuration = optimalPath.stream().mapToInt(HubRoute::duration).sum(); // 총 시간 합산 (총 환적시간 포함 x)

        return new OptimalPathResult(originHubId, destinationHubId, totalDistance, totalDuration, totalCost, segments);
    }

    private int calculateTotalDuration(List<RouteNetworkEdge> optimalPath, Map<UUID, HubTransferMetric> transferMetricsMap) {
        return optimalPath.stream().mapToInt(route -> {
            HubTransferMetric destMetric = transferMetricsMap.get(route.destinationHubId());
            int transferTime = (destMetric != null) ? destMetric.getExpectedTransferTime() : 0;
            return route.duration() + transferTime;
        }).sum();
    }
}
