package com.sixro.logistics.hub.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.domain.model.Address;
import com.sixro.logistics.hub.domain.model.Hub;
import com.sixro.logistics.hub.domain.model.Location;
import com.sixro.logistics.hub.domain.repository.HubRepository;
import com.sixro.logistics.hub.presentation.auth.Requester;
import com.sixro.logistics.hub.presentation.dto.HubDto;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HubCommandService {

    private final HubRepository hubRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326); // PostGIS Point 생성을 위한 Factory (SRID 4326: WGS84 위경도 좌표계)

    // TODO: Redis 캐싱 CacheEvict 추가

    public UUID createHub(HubDto.CreateRequest request) {
        if (hubRepository.existsByHubName(request.hubName())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        Hub hub = Hub.builder()
                .hubName(request.hubName())
                .address(Address.of(request.zipcode(), request.roadAddress(), request.jibunAddress(), request.detailAddress()))
                .location(Location.of(request.longitude(), request.latitude()))
                .hubZone(request.hubZone())
                .maxCapacity(request.maxCapacity())
                .build();

        UUID savedId = hubRepository.save(hub).getId();

        return savedId;
    }

    public UUID updateHub(UUID hubId, HubDto.UpdateRequest request) {
        Hub hub = getHubOrThrow(hubId);

        // 이름이 변경되었을 경우 중복 체크
        if (!hub.getHubName().equals(request.hubName()) && hubRepository.existsByHubName(request.hubName())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        hub.update(
                request.hubName(),
                Address.of(request.zipcode(), request.roadAddress(), request.jibunAddress(), request.detailAddress()),
                Location.of(request.longitude(), request.latitude()),
                request.hubZone(),
                request.maxCapacity()
        );

        return hub.getId();
    }

    public UUID changeHubStatus(UUID hubId, HubDto.StatusUpdateRequest request, Requester requester) {
        if (requester.isHubAdmin() && !hubId.equals(requester.affiliationId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Hub hub = getHubOrThrow(hubId);
        hub.changeStatus(request.hubStatus()); // 상태 전이 규칙 검증

        // TODO: HubStatusChangedEvent 발행 처리

        return hub.getId();
    }

    public void deleteHub(UUID hubId, Requester requester) {
        Hub hub = getHubOrThrow(hubId);

        hub.softDelete(requester.userId());

        // TODO: 캐시 무효화 및 연관된 하위 데이터(HubRoute 등) soft delete
    }

    private Hub getHubOrThrow(UUID hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
    }

    private Point createPoint(double longitude, double latitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}