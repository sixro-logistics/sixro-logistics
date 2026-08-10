package com.sixro.logistics.common.test.config;

import com.sixro.logistics.common.test.ContainerImages;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;

public class KafkaTestContainerConfig {

    @Container
    @ServiceConnection
    public static final KafkaContainer KAFKA =
            new KafkaContainer(ContainerImages.KAFKA);

}