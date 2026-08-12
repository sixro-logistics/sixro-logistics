package com.sixro.logistics.hub.hub.application.command;

import com.sixro.logistics.hub.hub.application.port.HubMetricsPort;
import com.sixro.logistics.hub.hub.domain.model.Hub;
import com.sixro.logistics.hub.hub.domain.model.HubMetric;
import com.sixro.logistics.hub.hub.domain.repository.HubMetricCommandRepository;
import com.sixro.logistics.hub.hub.domain.repository.HubMetricQueryRepository;
import com.sixro.logistics.hub.hub.domain.repository.HubQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubVolumeSnapshotScheduler {

    private final HubMetricsPort hubMetricsPort;
    private final HubQueryRepository hubQueryRepository;
    private final HubMetricQueryRepository metricQueryRepository;
    private final HubMetricCommandRepository metricCommandRepository;


    /**
     * Redis 물동량 캐싱 Write-Behind
     */
    @Scheduled(cron = "0 0/5 * * * *") // 매 5분마다 DB 스냅샷 백업
    @Transactional
    public void syncRedisVolumeToDb() {
        log.info("[Hub 물동량 DB 백업 배치] 시작");

        List<Hub> operatingHubs = hubQueryRepository.findAllOperatingHubs();
        List<UUID> hubIds = operatingHubs.stream().map(Hub::getId).toList();
        if (hubIds.isEmpty()) return;

        // Redis에서 현재 물동량 조회
        Map<UUID, Integer> redisVolumes = hubMetricsPort.getCurrentVolumes(hubIds);

        // Redis 데이터가 하나도 없으면 장애 또는 초기화 상태이므로 덮어쓰기 스킵
        if (redisVolumes.isEmpty()) {
            log.warn("[Hub 물동량 DB 백업 배치] 스킵 - Redis 데이터가 존재하지 않습니다.");
            return;
        }

        // 스냅샷 조회
        Map<UUID, HubMetric> dbMetricMap = metricQueryRepository.findAllByHubIdIn(hubIds).stream()
                .collect(Collectors.toMap(HubMetric::getHubId, m -> m));

        // 엔티티 생성 및 업데이트 매핑
        List<HubMetric> metricsToSave = new ArrayList<>();
        for (Hub hub : operatingHubs) {
            Integer volume = redisVolumes.get(hub.getId());

            // Redis에 값이 있는 허브만 DB 동기화 진행
            if (volume != null) {
                HubMetric metric = dbMetricMap.get(hub.getId());
                if (metric == null) {
                    metricsToSave.add(new HubMetric(hub, volume));
                } else {
                    metric.updateVolume(volume);
                    metricsToSave.add(metric);
                }
            }
        }

        metricCommandRepository.saveAll(metricsToSave);
        log.info("[Hub 물동량 DB 백업 배치] 종료 - 동기화 대상 허브 수: {}", metricsToSave.size());
    }
}