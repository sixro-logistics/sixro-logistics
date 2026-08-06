package com.sixro.logistics.hub.application.query;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.domain.model.Hub;
import com.sixro.logistics.hub.domain.model.HubWithDistance;
import com.sixro.logistics.hub.domain.model.HubZone;
import com.sixro.logistics.hub.domain.repository.HubRepository;
import com.sixro.logistics.hub.presentation.dto.HubDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubQueryService {

    private final HubRepository hubRepository;

    // TODO: Redis Look-Aside 캐싱 추가

    public HubDto.Response getHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return mapToResponse(hub);
    }

    public PageResponse<HubDto.Response> searchHubs(HubZone hubZone, String hubName, Pageable pageable) {
        Page<Hub> hubPage = hubRepository.search(hubZone, hubName, pageable);

        return PageResponse.from(hubPage, this::mapToResponse);
    }

    // TODO: Hub Route 데이터 추가 후, 비즈니스 로직 개선
    public HubDto.NearestResponse getNearestHub(double longitude, double latitude) {
        HubWithDistance hubWithDistance = hubRepository.findNearestHubWithDistance(longitude, latitude)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return new HubDto.NearestResponse(
                hubWithDistance.hubId(),
                hubWithDistance.hubName(),
                hubWithDistance.hubStatus(),
                hubWithDistance.distanceInMeters()
        );
    }

    private HubDto.Response mapToResponse(Hub hub) {
        return new HubDto.Response(
                hub.getId(),
                hub.getHubName(),
                hub.getAddress().getZipcode(),
                hub.getAddress().getAddress(),
                hub.getAddress().getDetailAddress(),
                hub.getLocation().getX(), // Longitude
                hub.getLocation().getY(), // Latitude
                hub.getHubZone(),
                hub.getMaxCapacity(),
                hub.getHubStatus()
        );
    }
}