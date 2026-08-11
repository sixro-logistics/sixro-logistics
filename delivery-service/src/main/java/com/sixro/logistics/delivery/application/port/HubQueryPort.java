package com.sixro.logistics.delivery.application.port;

import com.sixro.logistics.delivery.application.model.HubInfo;

import java.util.Optional;
import java.util.UUID;

// app -> infra 요구 기능 정의
public interface HubQueryPort {

    Optional<HubInfo> findHub(UUID hubId);
}
