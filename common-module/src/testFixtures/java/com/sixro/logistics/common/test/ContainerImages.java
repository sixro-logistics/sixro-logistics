package com.sixro.logistics.common.test;

import org.testcontainers.utility.DockerImageName;

public final class ContainerImages {

    private ContainerImages() {
    }

    public static final DockerImageName POSTGRES =
            DockerImageName.parse("postgis/postgis:18-3.6-alpine")
                    .asCompatibleSubstituteFor("postgres");

    public static final DockerImageName REDIS =
            DockerImageName.parse("redis:8.4-alpine");

    public static final DockerImageName KAFKA =
            DockerImageName.parse("apache/kafka:4.1.0");

}