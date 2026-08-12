package com.sixro.logistics.order.infrastructure.config;

import com.sixro.logistics.common.persistence.audit.CustomAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "customAuditorAware")
public class JpaAuditingConfig {

    @Bean
    public CustomAuditorAware customAuditorAware() {
        return new CustomAuditorAware();
    }
}