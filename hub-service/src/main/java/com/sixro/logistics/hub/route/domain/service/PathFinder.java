package com.sixro.logistics.hub.route.domain.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.strategy.RoutingWeightStrategy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PathFinder {

    private record NodeState(UUID hubId, double totalWeight) {}

    public List<HubRoute> findOptimalPath(
            List<HubRoute> allRoutes,
            UUID originHubId,
            UUID destinationHubId,
            RoutingWeightStrategy weightStrategy,
            Map<UUID, HubTransferMetric> transferMetrics
    ) {
        // 인접 리스트 그래프 구성
        Map<UUID, List<HubRoute>> graph = new HashMap<>();
        for (HubRoute route : allRoutes) {
            graph.computeIfAbsent(route.getOriginHubId(), k -> new ArrayList<>()).add(route);
        }

        // 초기화
        PriorityQueue<NodeState> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.totalWeight));
        Map<UUID, Double> minWeightMap = new HashMap<>();
        Map<UUID, HubRoute> edgeToMap = new HashMap<>(); // 경로 역추적용

        pq.add(new NodeState(originHubId, 0.0));
        minWeightMap.put(originHubId, 0.0);

        // 탐색 수행
        while (!pq.isEmpty()) {
            NodeState current = pq.poll();

            // 가중치가 낮은 경로 방문한 적이 있다면 스킵
            if (current.totalWeight() > minWeightMap.getOrDefault(current.hubId(), Double.MAX_VALUE)) {
                continue;
            }

            // 목적지 도달 시 탐색 종료 (최단 경로 보장)
            if (current.hubId().equals(destinationHubId)) {
                break;
            }

            // 인접 노드 탐색
            List<HubRoute> adjacentRoutes = graph.getOrDefault(current.hubId(), Collections.emptyList());
            for (HubRoute edge : adjacentRoutes) {
                double newWeight = current.totalWeight() + weightStrategy.calculateWeight(edge, transferMetrics);
                UUID nextHubId = edge.getDestinationHubId();

                if (newWeight < minWeightMap.getOrDefault(nextHubId, Double.MAX_VALUE)) {
                    minWeightMap.put(nextHubId, newWeight);
                    edgeToMap.put(nextHubId, edge); // 경로 기록
                    pq.add(new NodeState(nextHubId, newWeight));
                }
            }
        }

        // 도착지 -> 출발지 순서로 역추적
        if (!minWeightMap.containsKey(destinationHubId)) {
            // 목적지에 도달할 수 없는 경우
            throw new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        List<HubRoute> optimalPath = new ArrayList<>();
        UUID currentTracker = destinationHubId;

        while (!currentTracker.equals(originHubId)) {
            HubRoute route = edgeToMap.get(currentTracker);
            optimalPath.add(route);
            currentTracker = route.getOriginHubId();
        }

        // 출발지 -> 도착지 순서로 정리
        Collections.reverse(optimalPath);
        return optimalPath;
    }
}