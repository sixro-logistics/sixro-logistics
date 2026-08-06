package com.sixro.logistics.common.persistence.audit;

import com.sixro.logistics.common.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
public class CustomAuditorAware implements AuditorAware<UUID> {
    @Override
    public Optional<UUID> getCurrentAuditor() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userIdStr = request.getHeader(HeaderConstants.USER_ID);

            if (userIdStr != null && !userIdStr.isBlank()) {
                return Optional.of(UUID.fromString(userIdStr));
            }
        }
        return Optional.empty();
    }
}