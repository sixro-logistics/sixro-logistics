package com.sixro.logistics.hub.route.domain.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class HubNetworkTopologyPolicy {

    // 허브 연결망 정보
    private static final Map<String, List<String>> TOPOLOGY = Map.of(
            "경기 남부 센터", List.of("경기 북부 센터", "서울특별시 센터", "인천광역시 센터", "강원특별자치도 센터", "경상북도 센터", "대전광역시 센터", "대구광역시 센터"),
            "대전광역시 센터", List.of("충청남도 센터", "충청북도 센터", "세종특별자치시 센터", "전북특별자치도 센터", "광주광역시 센터", "전라남도 센터", "경기 남부 센터", "대구광역시 센터"),
            "대구광역시 센터", List.of("경상북도 센터", "경상남도 센터", "부산광역시 센터", "울산광역시 센터", "경기 남부 센터", "대전광역시 센터"),
            "경상북도 센터", List.of("경기 남부 센터", "대구광역시 센터")
    );

    public record RoutePair(String originName, String destinationName) {}

    // 양방향 노선 목록을 생성하여 반환
    public static Set<RoutePair> getInitialRoutePairs() {
        Set<RoutePair> targetRoutes = new HashSet<>();
        for (var entry : TOPOLOGY.entrySet()) {
            String origin = entry.getKey();
            for (String destination : entry.getValue()) {
                targetRoutes.add(new RoutePair(origin, destination));
                targetRoutes.add(new RoutePair(destination, origin));
            }
        }
        return targetRoutes;
    }
}