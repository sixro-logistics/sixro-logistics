package com.sixro.logistics.delivery;

import com.sixro.logistics.common.test.config.KafkaTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers(KafkaTestContainerConfig.class)
class DeliveryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
