package com.sixro.logistics.hub.hub.application.query;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubInternalQueryService {

    public static final String HUB_INFO_INTERNAL = "hub:info:internal";

    private final HubQueryRepository hubQueryRepository;

    @Cacheable(cacheNames = HUB_INFO_INTERNAL, key = "#hubId")
    public HubInternalInfo getHub(UUID hubId) {
        Hub hub = hubQueryRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return new HubInternalInfo(
                hub.getId(),
                hub.getHubName()
        );
    }
}