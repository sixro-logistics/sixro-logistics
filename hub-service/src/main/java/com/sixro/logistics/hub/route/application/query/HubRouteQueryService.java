package com.sixro.logistics.hub.route.application.query;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.repository.HubRouteQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteQueryService {

    public static final String HUB_ROUTE = "hub:route";

    private final HubRouteQueryRepository hubRouteQueryRepository;
    private final HubInfoPort hubInfoPort;

    @Cacheable(cacheNames = HUB_ROUTE, key = "#hubRouteId")
    public HubRouteDetailInfo getRoute(UUID hubRouteId) {
        HubRoute hubRoute = hubRouteQueryRepository.findById(hubRouteId)
                .orElseThrow(() -> new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));

        Map<UUID, String> hubNames = hubInfoPort.getHubNames(
                Set.of(hubRoute.getOriginHubId(), hubRoute.getDestinationHubId())
        );

        return new HubRouteDetailInfo(
                hubRoute.getId(),
                hubRoute.getOriginHubId(),
                hubNames.getOrDefault(hubRoute.getOriginHubId(), "알수없는 허브"),
                hubRoute.getDestinationHubId(),
                hubNames.getOrDefault(hubRoute.getDestinationHubId(), "알수없는 허브"),
                hubRoute.getDistance(),
                hubRoute.getDuration(),
                hubRoute.getRouteCost().getBaseCost(),
                hubRoute.getRouteCost().getTollFee(),
                GeoJsonLineString.from(hubRoute.getRoutePath()),
                hubRoute.getCreatedAt(),
                hubRoute.getCreatedBy(),
                hubRoute.getUpdatedAt(),
                hubRoute.getUpdatedBy()
        );
    }

    public Page<HubRouteInfo> searchRoutes(UUID originHubId, UUID destinationHubId, Pageable pageable) {
        Page<HubRoute> hubRoutePage = hubRouteQueryRepository.search(originHubId, destinationHubId, pageable);

        // 조회된 Page의 모든 출/도착지 HubId를 추출하여 한 번에 조회
        Set<UUID> hubIdsToFetch = new HashSet<>();
        hubRoutePage.forEach(route -> {
            hubIdsToFetch.add(route.getOriginHubId());
            hubIdsToFetch.add(route.getDestinationHubId());
        });

        Map<UUID, String> hubNames = hubInfoPort.getHubNames(hubIdsToFetch);

        return hubRoutePage.map(route -> new HubRouteInfo(
                route.getId(),
                route.getOriginHubId(),
                hubNames.getOrDefault(route.getOriginHubId(), "Unknown"),
                route.getDestinationHubId(),
                hubNames.getOrDefault(route.getDestinationHubId(), "Unknown"),
                route.getDistance(),
                route.getDuration(),
                route.getRouteCost().getBaseCost(),
                route.getRouteCost().getTollFee(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        ));
    }
}