package com.sixro.logistics.hub;

import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import com.sixro.logistics.common.test.config.PostgresTestContainerConfig;
import com.sixro.logistics.common.test.config.RedisTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers({
        PostgresTestContainerConfig.class,
        RedisTestContainerConfig.class,
        KafkaTestContainerConfig.class
})
class HubServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
