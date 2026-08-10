package com.sixro.logistics.hub.integration;

import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.common.test.config.RedisTestContainerConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        RedisTestContainerConfig.class
})
class RedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("Redis에 데이터를 저장하고 정상적으로 조회되는지 확인한다")
    void redis_save_and_read_test() {

        String key = "hub:test";
        String value = "seoul-hub";

        redisTemplate.opsForValue()
                .set(key, value);

        String result =
                redisTemplate.opsForValue()
                        .get(key);

        assertThat(result)
                .isEqualTo(value);
    }
}