package com.sixro.logistics.hub.hub.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.hub.common.auth.UserContext;
import com.sixro.logistics.hub.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.hub.domain.model.Address;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.Location;
import com.sixro.logistics.hub.hub.domain.repository.HubCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HubCommandService {

    private static final String HUB_INFO = "hub:info";
    public static final String HUB_INFO_INTERNAL = "hub:info:internal";

    private final HubCommandRepository hubCommandRepository;

    public UUID createHub(HubCommand.Create command) {
        if (hubCommandRepository.existsByHubName(command.hubName())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        Hub hub = Hub.builder()
                .hubName(command.hubName())
                .address(Address.of(command.zipcode(), command.roadAddress(), command.jibunAddress(), command.detailAddress()))
                .location(Location.of(command.longitude(), command.latitude()))
                .hubZone(command.hubZone())
                .maxCapacity(command.maxCapacity())
                .build();

        return hubCommandRepository.save(hub).getId();
    }

    @CacheEvict(cacheNames = {HUB_INFO, HUB_INFO_INTERNAL}, key = "#hubId")
    public UUID updateHub(UUID hubId, HubCommand.Update command) {
        Hub hub = getHubOrThrow(hubId);

        if (!hub.getHubName().equals(command.hubName()) && hubCommandRepository.existsByHubName(command.hubName())) {
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

    @CacheEvict(cacheNames = {HUB_INFO, HUB_INFO_INTERNAL}, key = "#hubId")
    public UUID changeHubStatus(UUID hubId, HubCommand.ChangeStatus command, UserContext userContext) {
        if (userContext.isHubAdmin() && !hubId.equals(userContext.affiliationId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Hub hub = getHubOrThrow(hubId);
        hub.changeStatus(command.hubStatus());

        // TODO: 수정 이벤트 발행 -> 전체 캐시 무효화, CLOSED 상태로 변화시 상세 처리 필요

        return hub.getId();
    }

    @CacheEvict(cacheNames = {HUB_INFO, HUB_INFO_INTERNAL}, key = "#hubId")
    public void deleteHub(UUID hubId, UserContext userContext) {
        Hub hub = getHubOrThrow(hubId);
        hub.softDelete(userContext.userId());

        // TODO: 삭제 이벤트 발행 -> 연관 데이터 soft delete, 전체 캐시 무효화
    }

    private Hub getHubOrThrow(UUID hubId) {
        return hubCommandRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
    }
}