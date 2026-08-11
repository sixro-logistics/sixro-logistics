package com.sixro.logistics.hub.route.infrastructure.client.hub;

import com.sixro.logistics.hub.hub.application.query.HubQueryService;
import com.sixro.logistics.hub.hub.presentation.dto.HubDto;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubInfoAdapter implements HubInfoPort {

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
}