package com.sixro.logistics.hub.route.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteKafkaEventListener {

    private static final String HUB_INFO_CLOSED = "hub:info:closed";
    private static final String TOPIC_HUB_STATUS = "hub-status-changed";
    private static final String ROUTE_GROUP_ID = "route-service-group";

    private final CacheManager cacheManager;

    /**
     * 허브 상태가 CLOSED로 변경되면, 토폴로지가 바뀌므로 캐시 무효화
     */
    @KafkaListener(topics = TOPIC_HUB_STATUS, groupId = ROUTE_GROUP_ID)
    public void handleHubStatusChangedEvent(HubStatusEventPayload payload) {

        if (isTopologyChanged(payload.previousStatus(), payload.newStatus())) {

            // 다익스트라 연산용 캐시 무효화
            Objects.requireNonNull(cacheManager.getCache(HUB_INFO_CLOSED)).clear();

            log.info("[Route 다익스트라 캐시 무효화] 허브 차단/복구 감지 - HubID: {}, {} -> {}",
                    payload.hubId(), payload.previousStatus(), payload.newStatus());
        } else {
            log.debug("[Route 캐시 유지] 토폴로지 변경 없음 - HubID: {}, {} -> {}",
                    payload.hubId(), payload.previousStatus(), payload.newStatus());
        }
    }

    private boolean isTopologyChanged(String prev, String next) {
        // 토폴로지(허브망) 변경 조건:
        // 1. 운영 중단: [ACTIVE, CONGESTED, MAINTENANCE] -> [CLOSED]
        // 2. 운영 복구: [CLOSED] -> [MAINTENANCE]
        return "CLOSED".equals(prev) || "CLOSED".equals(next);
    }
}