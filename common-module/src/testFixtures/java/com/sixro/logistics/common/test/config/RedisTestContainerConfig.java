package com.sixro.logistics.common.test.config;

import com.sixro.logistics.common.test.ContainerImages;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

public class RedisTestContainerConfig {

    @Container
    @ServiceConnection(name = "redis")
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(ContainerImages.REDIS)
                    .withExposedPorts(6379);

}