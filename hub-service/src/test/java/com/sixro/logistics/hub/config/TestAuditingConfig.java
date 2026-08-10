package com.sixro.logistics.hub.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@TestConfiguration
@EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
public class TestAuditingConfig {

    @Bean()
    @Primary
    public AuditorAware<UUID> testAuditorAware() {
        return () -> Optional.of(UUID.randomUUID());
    }
}