package com.sixro.logistics.common.test.config;

import com.sixro.logistics.common.test.ContainerImages;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class PostgresTestContainerConfig {

    @Container
    @ServiceConnection
    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(ContainerImages.POSTGRES)
                    .withDatabaseName("sixro_db")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("init.sql");

}