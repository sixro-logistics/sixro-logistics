package com.sixro.logistics.hub.hub.infrastructure.redis;

import com.sixro.logistics.hub.hub.application.port.HubMetricsPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class HubRedisMetricsAdapter implements HubMetricsPort {

    private static final String HUB_METRICS_VOLUME = "hub:metrics:volume";

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> deductScript; // 애플리케이션 기동 시 Lua 스크립트 로드

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/deduct-volume.lua")));
        deductScript.setResultType(Long.class);
    }

    @Override
    public void increaseVolume(UUID hubId, int boxCount) {
        // 허브 물동량 증가
        redisTemplate.opsForValue().increment(generateKey(hubId), boxCount);
    }

    @Override
    public int decreaseVolume(UUID hubId, int maxDeductAmount) {
        // 허브 물동량 차감
        Long remainingVolume = redisTemplate.execute(
                deductScript,
                Collections.singletonList(generateKey(hubId)),
                String.valueOf(maxDeductAmount)
        );

        return remainingVolume != null ? remainingVolume.intValue() : 0;
    }

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

    private String generateKey(UUID hubId) {
        return HUB_METRICS_VOLUME + ":" + hubId;
    }
}