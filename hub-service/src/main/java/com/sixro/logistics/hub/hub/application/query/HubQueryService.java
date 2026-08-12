package com.sixro.logistics.hub.hub.application.query;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.hub.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.hub.domain.model.HubZone;
import com.sixro.logistics.hub.hub.domain.repository.HubMetricQueryRepository;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import com.sixro.logistics.hub.hub.presentation.dto.HubDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubQueryService {

    public static final String HUB_INFO = "hub:info";

    private final HubQueryRepository hubQueryRepository;
    private final HubMetricQueryRepository hubMetricQueryRepository; // [추가됨]

    @Cacheable(cacheNames = HUB_INFO, key = "#hubId")
    public HubDto.Response getHub(UUID hubId) {
        Hub hub = hubQueryRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return mapToResponse(hub);
    }

    public PageResponse<HubDto.Response> searchHubs(HubZone hubZone, String hubName, Pageable pageable) {
        Page<Hub> hubPage = hubQueryRepository.search(hubZone, hubName, pageable);
        return PageResponse.from(hubPage, this::mapToResponse);
    }

    public HubDto.NearestResponse getNearestHub(double longitude, double latitude) {
        HubWithDistance hubWithDistance = hubQueryRepository.findNearestHubWithDistance(longitude, latitude)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return new HubDto.NearestResponse(
                hubWithDistance.hubId(),
                hubWithDistance.hubName(),
                hubWithDistance.hubStatus(),
                hubWithDistance.distanceInMeters()
        );
    }

    public List<HubDto.Response> getHubsByIds(Set<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return List.of();
        }

        List<Hub> hubs = hubQueryRepository.findByIdIn(hubIds);

        return hubs.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<HubDto.MetricResponse> getHubMetricsByIds(Set<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return List.of();
        }

        // Set을 List로 변환하여 인프라 어댑터에 전달
        return hubMetricQueryRepository.findAllByHubIdIn(hubIds.stream().toList())
                .stream()
                .map(metric -> new HubDto.MetricResponse(
                        metric.getHubId(),
                        metric.getCurrentVolume()
                ))
                .toList();
    }

    private HubDto.Response mapToResponse(Hub hub) {
        return new HubDto.Response(
                hub.getId(),
                hub.getHubName(),
                hub.getAddress().getZipcode(),
                hub.getAddress().getRoadAddress(),
                hub.getAddress().getJibunAddress(),
                hub.getAddress().getDetailAddress(),
                hub.getLocation().getLongitude(),
                hub.getLocation().getLatitude(),
                hub.getHubZone(),
                hub.getMaxCapacity(),
                hub.getHubStatus()
        );
    }
}