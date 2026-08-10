package com.sixro.logistics.hub.route.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.repository.HubRouteQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouteBatchService {

    private final HubRouteQueryRepository hubRouteQueryRepository;
    private final HubRouteCommandService hubRouteCommandService;
    private final HubInfoPort hubInfoPort;
    private final AtomicBoolean isRunning = new AtomicBoolean(false); // 실행 상태 락

    @Async
    public void syncAllRoutesBackground() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("[HubRoute 전체 동기화 배치] 이미 실행 중입니다. 중복 요청을 무시합니다.");
            return;
        }

        try {
            log.info("[HubRoute 전체 동기화 배치] 시작");

            // 노선 목록 조회
            List<HubRoute> activeRoutes = hubRouteQueryRepository.search(null, null, Pageable.unpaged()).getContent();

            // 전체 허브 목록을 가져와 Batch 목적에 맞게 Map
            Map<UUID, HubInfoPort.HubCoordinate> coordinatesMap = hubInfoPort.getAllHubs().stream()
                    .collect(Collectors.toMap(
                            HubInfoPort.HubBasicInfo::hubId,
                            hub -> new HubInfoPort.HubCoordinate(hub.longitude(), hub.latitude())
                    ));

            int successCount = 0, skipCount = 0;
            List<UUID> changedRouteIds = new ArrayList<>(); // 변경된 ID 수집 (벌크 이벤트용)

            for (HubRoute route : activeRoutes) {
                try {
                    HubInfoPort.HubCoordinate origin = coordinatesMap.get(route.getOriginHubId());
                    HubInfoPort.HubCoordinate dest = coordinatesMap.get(route.getDestinationHubId());

                    if (origin == null || dest == null) {
                        log.warn("[HubRoute 전체 동기화 배치] 좌표 매핑 실패 - RouteID: {}", route.getId());
                        skipCount++;
                        continue;
                    }

                    // 동기화
                    boolean isChanged = hubRouteCommandService.syncRouteForBatch(route, origin, dest, 2);

                    if (isChanged) {
                        changedRouteIds.add(route.getId());
                    }

                    successCount++;
                    Thread.sleep(500); // Rate Limit 방어

                } catch (BaseException e) {
                    log.warn("[HubRoute 전체 동기화 배치] 스킵 - RouteID: {}, 사유: {}", route.getId(), e.getMessage());
                    skipCount++;
                } catch (Exception e) {
                    log.error("[HubRoute 전체 동기화 배치] 실패 (알 수 없는 오류) - RouteID: {}", route.getId(), e);
                    skipCount++;
                }
            }

            log.info("[HubRoute 전체 동기화 배치] 종료 - 대상: {}, 성공: {}, 스킵/실패: {}, 변경된 노선 수: {}",
                    activeRoutes.size(), successCount, skipCount, changedRouteIds.size());

            // Batch 종료 후 대규모 변경이 있다면 벌크 이벤트 1회 발행
            if (!changedRouteIds.isEmpty()) {
                log.info("[HubRoute 전체 동기화 배치] 대규모 변경 감지. 벌크 이벤트 발행 및 전체 캐시 무효화 진행");
                // TODO: HubRoutesBulkChangedEvent 발행 (changedRouteIds 포함)
                // TODO: 노선 목록 관련 캐시 Evict 수행
            }

        } finally {
            isRunning.set(false); // 락 해제
        }
    }
}