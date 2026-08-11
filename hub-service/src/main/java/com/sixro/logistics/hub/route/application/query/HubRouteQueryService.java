package com.sixro.logistics.hub.route.application.query;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.repository.HubRouteQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteQueryService {

    private final HubRouteQueryRepository hubRouteQueryRepository;

    // TODO: Redis Look-Aside 캐싱 추가

    public HubRouteDetailInfo getRoute(UUID hubRouteId) {
        HubRoute hubRoute = hubRouteQueryRepository.findById(hubRouteId)
                .orElseThrow(() -> new BaseException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));

        return new HubRouteDetailInfo(
                hubRoute.getId(),
                hubRoute.getOriginHubId(),
                hubRoute.getDestinationHubId(),
                hubRoute.getDistance(),
                hubRoute.getDuration(),
                hubRoute.getRouteCost().getTollFee(),
                hubRoute.getRouteCost().getBaseCost(),
                hubRoute.getRoutePath()
        );
    }

    public Page<HubRouteInfo> searchRoutes(UUID originHubId, UUID destinationHubId, Pageable pageable) {
        Page<HubRoute> hubRoutePage = hubRouteQueryRepository.search(originHubId, destinationHubId, pageable);

        return hubRoutePage.map(route -> new HubRouteInfo(
                route.getId(),
                route.getOriginHubId(),
                route.getDestinationHubId(),
                route.getDistance(),
                route.getDuration(),
                route.getRouteCost().getTollFee(),
                route.getRouteCost().getBaseCost()
        ));
    }
}