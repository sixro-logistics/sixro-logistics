package com.sixro.logistics.hub.route.infrastructure.redis;

import com.sixro.logistics.hub.route.application.port.RouteMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RouteRedisMetricsAdapter implements RouteMetricsPort {

    private static final String HUB_METRICS_VOLUME = "hub:metrics:volume";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Map<UUID, Integer> getCurrentVolumes(List<UUID> hubIds) {
        if (hubIds == null || hubIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 조회할 Redis Key 리스트
        List<String> keys = hubIds.stream()
                .map(this::generateKey)
                .toList();

        // MGET 으로 모든 허브 물동량 일괄 조회
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<UUID, Integer> volumeMap = new HashMap<>();

        // hubId 와 volume 매핑
        for (int i = 0; i < hubIds.size(); i++) {
            String val = (values != null) ? values.get(i) : null;
            if (val != null) {
                volumeMap.put(hubIds.get(i), Integer.parseInt(val));
            }
        }
        return volumeMap;
    }

    @Override
    public void setVolume(UUID hubId, int volume) {
        redisTemplate.opsForValue().set(generateKey(hubId), String.valueOf(volume));
    }

    private String generateKey(UUID hubId) {
        return HUB_METRICS_VOLUME + ":" + hubId;
    }
}