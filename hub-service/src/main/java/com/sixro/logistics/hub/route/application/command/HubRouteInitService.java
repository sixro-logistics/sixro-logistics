package com.sixro.logistics.hub.route.application.command;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.application.port.HubInfoPort;
import com.sixro.logistics.hub.route.domain.policy.RouteNetworkPolicy;
import com.sixro.logistics.hub.route.domain.policy.RouteNetworkPolicy.RoutePair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouteInitService {

    public static final String HUB_ROUTE = "hub:route";
    public static final String HUB_NETWORK_ACTIVE = "hub:network:active";

    private final HubInfoPort hubInfoPort;
    private final HubRouteCommandService hubRouteCommandService;
    private final CacheManager cacheManager;
    private final AtomicBoolean isRunning = new AtomicBoolean(false); // 실행 상태 락(Lock)

    @Async
    public void initializeRoutesBackground() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("[HubRoute 초기화 배치] 이미 실행 중입니다. 중복 요청을 무시합니다.");
            return;
        }

        try {
            log.info("[HubRoute 초기화 배치] 시작");

            // 전체 허브 목록을 가져와 Init 목적에 맞게 Map
            Map<String, UUID> hubNameIdMap = hubInfoPort.getAllHubs().stream()
                    .collect(Collectors.toMap(
                            HubInfoPort.HubBasicInfo::hubName,
                            HubInfoPort.HubBasicInfo::hubId
                    ));

            // 양방향 노선 목록 가져오기
            Set<RoutePair> targetRoutes = RouteNetworkPolicy.getInitialRoutePairs();

            int successCount = 0, failCount = 0;

            for (RoutePair pair : targetRoutes) {
                try {
                    UUID originId = hubNameIdMap.get(pair.originName());
                    UUID destinationId = hubNameIdMap.get(pair.destinationName());

                    if (originId == null || destinationId == null) {
                        log.warn("[HubRoute 초기화 배치] 허브 매핑 실패 - {} ➔ {}", pair.originName(), pair.destinationName());
                        failCount++;
                        continue;
                    }

                    hubRouteCommandService.createRoute(new CreateHubRouteCommand(originId, destinationId));
                    successCount++;
                    log.info("[HubRoute 초기화 배치] 생성 성공 - {} ➔ {}", pair.originName(), pair.destinationName());

                    Thread.sleep(500); // Rate Limit 방어

                } catch (BaseException e) {
                    log.warn("[HubRoute 초기화 배치] 생성 스킵 - {} ➔ {}, 사유: {}", pair.originName(), pair.destinationName(), e.getMessage());
                    failCount++;
                } catch (Exception e) {
                    log.error("[HubRoute 초기화 배치] 생성 실패 - {} ➔ {}", pair.originName(), pair.destinationName(), e);
                    failCount++;
                }
            }
            log.info("[HubRoute 초기화 배치] 종료 - 시도: {}, 성공: {}, 스킵/실패: {}", targetRoutes.size(), successCount, failCount);

            if (successCount > 0) {
                log.info("[HubRoute 초기화 배치] 캐시 전체 무효화 수행");
                Objects.requireNonNull(cacheManager.getCache(HUB_ROUTE)).clear();
                Objects.requireNonNull(cacheManager.getCache(HUB_NETWORK_ACTIVE)).clear();
            }
        } finally {
            isRunning.set(false); // 락 해제
        }
    }
}