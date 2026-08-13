package com.sixro.logistics.hub.hub.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.hub.application.port.HubEventPort;
import com.sixro.logistics.hub.hub.application.port.HubMetricsPort;
import com.sixro.logistics.hub.hub.domain.event.HubStatusChangedEvent;
import com.sixro.logistics.hub.hub.domain.exception.HubErrorCode;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.hub.domain.repository.HubCommandRepository;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubVolumeIncreaseCommandService {

    private final HubMetricsPort hubMetricsPort;
    private final HubEventPort hubEventPort;
    private final HubQueryRepository hubQueryRepository;
    private final HubCommandRepository hubCommandRepository;

    @Transactional
    public void increaseAndEvaluateHubVolume(UUID hubId, int incomingVolume) {
        // 대상 허브 조회
        Hub hub = hubQueryRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (hub.getHubStatus() == HubStatus.CLOSED) {
            log.warn("CLOSED 상태인 허브({})로 물동량 유입 시도 방어", hub.getHubName());
            return;
        }

        // Redis 물동량 증가
        int currentVolume = hubMetricsPort.increaseVolume(hubId, incomingVolume);

        // 적재율 계산
        int storageCapacity = (int) (hub.getMaxCapacity() * 0.20);
        if (storageCapacity <= 0) storageCapacity = 2000; // MIN_CAPACITY 10_000 의 20% (0으로 나누기 방어)

        double utilization = (double) currentVolume / storageCapacity;

        // 상태 전이 검사
        HubStatus currentStatus = hub.getHubStatus();
        HubStatus newStatus = currentStatus.determineNewStatus(utilization, currentStatus);

        if (newStatus != null) {
            log.info("[Hub 적재율 급증 감지] 상태 변경 - Hub명: {}, 상태 변경: {} -> {}, 적재율: {}",
                    hub.getHubName(), currentStatus, newStatus, String.format("%.2f", utilization));

            // DB 업데이트
            hub.changeStatus(newStatus);
            hubCommandRepository.save(hub);

            // 허브상태 변경 이벤트 발행
            HubStatusChangedEvent event = HubStatusChangedEvent.of(hub.getId(), currentStatus, newStatus);
            hubEventPort.publishStatusChangedEvent(event);
        }
    }
}