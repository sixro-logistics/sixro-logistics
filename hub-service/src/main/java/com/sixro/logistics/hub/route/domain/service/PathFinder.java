package com.sixro.logistics.hub.route.domain.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubTransferMetric;
import com.sixro.logistics.hub.route.domain.model.RouteNetworkEdge;
import com.sixro.logistics.hub.route.domain.strategy.RoutingWeightStrategy;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 다익스트라 알고리즘 구현
 */
@Service
public class PathFinder {

    private record NodeState(UUID hubId, double totalWeight) {}

    public List<RouteNetworkEdge> findOptimalPath(
            List<RouteNetworkEdge> allEdges,
            UUID originHubId,
            UUID destinationHubId,
            RoutingWeightStrategy weightStrategy,
            Map<UUID, HubTransferMetric> transferMetrics
    ) {
        // Route Network (운영 노선망) 인접 리스트로 구성
        Map<UUID, List<RouteNetworkEdge>> graph = new HashMap<>();
        for (RouteNetworkEdge edge : allEdges) {
            graph.computeIfAbsent(edge.originHubId(), k -> new ArrayList<>()).add(edge);
        }

        // 초기화
        PriorityQueue<NodeState> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.totalWeight));
        Map<UUID, Double> minWeightMap = new HashMap<>();
        Map<UUID, RouteNetworkEdge> edgeToMap = new HashMap<>();    // 경로 역추적용

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
            List<RouteNetworkEdge> adjacentEdges = graph.getOrDefault(current.hubId(), Collections.emptyList());
            for (RouteNetworkEdge edge : adjacentEdges) {
                // TODO: 전략 패턴 파라미터도 RouteNetworkEdge로 통일되어야 합니다.
                double newWeight = current.totalWeight() + weightStrategy.calculateWeight(edge, transferMetrics);
                UUID nextHubId = edge.destinationHubId();

                if (newWeight < minWeightMap.getOrDefault(nextHubId, Double.MAX_VALUE)) {
                    minWeightMap.put(nextHubId, newWeight);
                    edgeToMap.put(nextHubId, edge);
                    pq.add(new NodeState(nextHubId, newWeight));
                }
            }
        }

        // 도착지 -> 출발지 순서로 역추적
        if (!minWeightMap.containsKey(destinationHubId)) {
            // 목적지에 도달할 수 없는 경우
            throw new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        List<RouteNetworkEdge> optimalPath = new ArrayList<>();
        UUID currentTracker = destinationHubId;

        while (!currentTracker.equals(originHubId)) {
            RouteNetworkEdge edge = edgeToMap.get(currentTracker);
            optimalPath.add(edge);
            currentTracker = edge.originHubId();
        }

        // 출발지 -> 도착지 순서로 정리
        Collections.reverse(optimalPath);
        return optimalPath;
    }
}