package com.sixro.logistics.hub.hub.application.command;

import com.sixro.logistics.hub.hub.application.port.HubEventPort;
import com.sixro.logistics.hub.hub.application.port.HubMetricsPort;
import com.sixro.logistics.hub.hub.domain.event.HubStatusChangedEvent;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.hub.domain.repository.HubCommandRepository;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubUtilizationMonitorScheduler {

    private static final int MINUTES_PER_DAY = 1440; // 1440분 = 24시간 = 1일

    private final HubMetricsPort hubMetricsPort;
    private final HubEventPort hubEventPort;
    private final HubQueryRepository hubQueryRepository;
    private final HubCommandRepository hubCommandRepository;

    /**
     * 물동량 자동 차감 시뮬레이션 및 물동량 변동에 따른 상태변화를 위한 스케줄러
     */
    @Scheduled(cron = "0 * * * * *") // 1분마다 물동량 차감 및 상태 처리
    @Transactional
    public void monitorAndAdjustHubUtilization() {
        // CLOSED 상태를 제외한 허브 조회
        List<Hub> operatingHubs = hubQueryRepository.findAllOperatingHubs();

        for (Hub hub : operatingHubs) {
            int maxCapacity = hub.getMaxCapacity(); // 최대 처리 용량
            int decreaseAmount = Math.max(maxCapacity / MINUTES_PER_DAY, 1); // 분당 처리량 (최대 처리 용량 / 1440분 으로 설정)

            // Redis Lua 스크립트 활용 물동량 차감
            int currentVolume = hubMetricsPort.decreaseVolume(hub.getId(), decreaseAmount);

            // 상태 변경 확인, 변경 발생시 이벤트 발행
            evaluateAndTransitionStatus(hub, currentVolume, maxCapacity);
        }
    }

    private void evaluateAndTransitionStatus(Hub hub, int currentVolume, int maxCapacity) {
        int storageCapacity = (int) (maxCapacity * 0.20);
        if (storageCapacity <= 0) storageCapacity = 2000;

        double utilization = (double) currentVolume / storageCapacity; // 적재율 = 물동량 / 최대 처리 용량
        HubStatus currentStatus = hub.getHubStatus();

        // 상태 전이 규칙 적용하여 상태 변화 판단
        HubStatus newStatus = currentStatus.determineNewStatus(utilization, currentStatus);

        if (newStatus != null) {
            log.info("[Hub 적재율 모니터링 배치] 상태 변경 - Hub명: {}, 상태 변경: {} -> {}, 적재율: {}",
                    hub.getHubName(), currentStatus, newStatus, String.format("%.2f", utilization));

            // DB 반영
            hub.changeStatus(newStatus);
            hubCommandRepository.save(hub);

            // 상태 변화 이벤트 발행
            HubStatusChangedEvent event = HubStatusChangedEvent.of(hub.getId(), currentStatus, newStatus);
            hubEventPort.publishStatusChangedEvent(event);
        }
    }
}