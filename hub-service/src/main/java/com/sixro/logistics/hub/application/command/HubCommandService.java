package com.sixro.logistics.hub.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.domain.model.Address;
import com.sixro.logistics.hub.domain.model.Hub;
import com.sixro.logistics.hub.domain.model.Location;
import com.sixro.logistics.hub.domain.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HubCommandService {

    private final HubRepository hubRepository;

    // TODO: Redis 캐싱 CacheEvict 추가

    public UUID createHub(HubCommand.Create command) {
        if (hubRepository.existsByHubName(command.hubName())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        Hub hub = Hub.builder()
                .hubName(command.hubName())
                .address(Address.of(command.zipcode(), command.roadAddress(), command.jibunAddress(), command.detailAddress()))
                .location(Location.of(command.longitude(), command.latitude()))
                .hubZone(command.hubZone())
                .maxCapacity(command.maxCapacity())
                .build();

        UUID savedId = hubRepository.save(hub).getId();

        return savedId;
    }

    public UUID updateHub(UUID hubId, HubCommand.Update command) {
        Hub hub = getHubOrThrow(hubId);

        // 이름이 변경되었을 경우 중복 체크
        if (!hub.getHubName().equals(command.hubName()) && hubRepository.existsByHubName(command.hubName())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        hub.update(
                command.hubName(),
                Address.of(command.zipcode(), command.roadAddress(), command.jibunAddress(), command.detailAddress()),
                Location.of(command.longitude(), command.latitude()),
                command.hubZone(),
                command.maxCapacity()
        );

        return hub.getId();
    }

    public UUID changeHubStatus(UUID hubId, HubCommand.ChangeStatus command, UserContext userContext) {
        if (userContext.isHubAdmin() && !hubId.equals(userContext.affiliationId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Hub hub = getHubOrThrow(hubId);
        hub.changeStatus(command.hubStatus()); // 상태 전이 규칙 검증

        // TODO: HubStatusChangedEvent 발행 처리

        return hub.getId();
    }

    public void deleteHub(UUID hubId, UserContext userContext) {
        Hub hub = getHubOrThrow(hubId);

        hub.softDelete(userContext.userId());

        // TODO: 캐시 무효화 및 연관된 하위 데이터(HubRoute 등) soft delete
    }

    private Hub getHubOrThrow(UUID hubId) {
        return hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
    }
}