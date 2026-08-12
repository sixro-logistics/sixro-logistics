package com.sixro.logistics.hub.route.infrastructure.client.hub;

import com.sixro.logistics.hub.hub.application.query.HubQueryService;
import com.sixro.logistics.hub.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.hub.presentation.dto.HubDto;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HubInfoAdapter implements HubInfoPort {

    private static final String HUB_INFO_CLOSED = "hub:info:closed";

    private final HubQueryService hubQueryService;

    @Override
    public HubCoordinate getHubCoordinate(UUID hubId) {
        // Hub 애그리거트의 서비스 호출
        HubDto.Response hubResponse = hubQueryService.getHub(hubId);

        return new HubCoordinate(
                hubResponse.longitude(),
                hubResponse.latitude()
        );
    }

    @Override
    public List<HubBasicInfo> getAllHubs() {
        // Hub 애그리거트의 서비스 호출
        return hubQueryService.searchHubs(null, null, Pageable.unpaged())
                .getContent()
                .stream()
                .map(hub -> new HubBasicInfo(
                        hub.hubId(),
                        hub.hubName(),
                        hub.longitude(),
                        hub.latitude()
                ))
                .toList();
    }

    /**
     * CLOSED 허브 목록 캐싱
     */
    @Override
    @Cacheable(cacheNames = HUB_INFO_CLOSED)
    public Set<UUID> findClosedHubIds() {
        // CLOSED인 허브의 ID만 필터링
        return hubQueryService.searchHubs(null, null, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(hub -> hub.hubStatus() == HubStatus.CLOSED)
                .map(HubDto.Response::hubId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<UUID, String> getHubNames(Set<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return Map.of();
        }

        return hubQueryService.getHubsByIds(hubIds).stream()
                .collect(Collectors.toMap(
                        HubDto.Response::hubId,
                        HubDto.Response::hubName
                ));
    }

    @Override
    public Map<UUID, Integer> getHubCapacities(List<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> uniqueHubIds = new HashSet<>(hubIds);

        return hubQueryService.getHubsByIds(uniqueHubIds).stream()
                .collect(Collectors.toMap(
                        HubDto.Response::hubId,
                        HubDto.Response::maxCapacity
                ));
    }

    @Override
    public Map<UUID, Integer> getHubVolumeSnapshots(List<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> uniqueHubIds = new HashSet<>(hubIds);

        return hubQueryService.getHubMetricsByIds(uniqueHubIds).stream()
                .collect(Collectors.toMap(
                        HubDto.MetricResponse::hubId,
                        HubDto.MetricResponse::currentVolume
                ));
    }
}