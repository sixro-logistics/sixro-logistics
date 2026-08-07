package com.sixro.logistics.common.persistence.autoconfigure;

import com.sixro.logistics.common.persistence.audit.CustomAuditorAware;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.UUID;

/**
 * Servlet 기반 JPA 서비스에서 사용할 공통 AuditorAware를 자동 구성합니다.
 *
 * <p>서비스가 별도의 AuditorAware를 정의하지 않은 경우에만
 * common-module의 CustomAuditorAware를 등록합니다.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnClass({
        AuditorAware.class,
        HttpServletRequest.class
})
public class CommonJpaAuditingAutoConfiguration {

    @Bean(name = "customAuditorAware")
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<UUID> auditorAware() {
        return new CustomAuditorAware();
    }
}