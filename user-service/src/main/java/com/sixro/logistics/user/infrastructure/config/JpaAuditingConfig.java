package com.sixro.logistics.user.infrastructure.config;

import com.sixro.logistics.common.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * User Service의 JPA Auditing 처리자 정보를 제공합니다.
 *
 * <p>Gateway가 전달한 X-User-Id를 createdBy와 updatedBy에 사용합니다.</p>
 */
@Configuration
public class JpaAuditingConfig {

    /**
     * 인증 사용자가 없는 시스템 작업에 사용하는 감사 ID입니다.
     *
     * <p>TODO Auth Service의 회원가입 내부 호출 방식이 확정되면
     * 시스템 사용자 ID 정책을 다시 검토합니다.</p>
     */
    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return Optional.of(SYSTEM_USER_ID);
            }

            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(HeaderConstants.USER_ID);

            if (userId == null || userId.isBlank()) {
                return Optional.of(SYSTEM_USER_ID);
            }

            try {
                return Optional.of(UUID.fromString(userId));
            } catch (IllegalArgumentException exception) {
                /*
                 * Gateway를 거치지 않은 잘못된 내부 Header인 경우
                 * 시스템 사용자로 처리합니다.
                 *
                 * TODO 내부 서비스 직접 접근 차단이 완료되면
                 * 잘못된 Header에 대해 예외 처리하는 방안도 검토합니다.
                 */
                return Optional.of(SYSTEM_USER_ID);
            }
        };
    }
}