package com.sixro.logistics.hub.hub.application.query;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubInternalQueryService {

    private final HubQueryRepository hubQueryRepository;

    public HubInternalInfo getHub(UUID hubId) {
        Hub hub = hubQueryRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        return new HubInternalInfo(
                hub.getId(),
                hub.getHubName()
        );
    }
}