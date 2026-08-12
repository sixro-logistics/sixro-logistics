package com.sixro.logistics.hub.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    // 기본 TTL (초 단위, 디폴트 1시간)
    private long defaultTtl = 3600;

    // 캐시 키별 맞춤 TTL (초 단위)
    private Map<String, Long> ttls = new HashMap<>();
}