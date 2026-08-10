package com.sixro.logistics.common.persistence.audit;

import com.sixro.logistics.common.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * 모든 서비스에서 공통으로 사용하는 JPA Auditing 사용자 식별자 제공자입니다.
 *
 * <p>Gateway에서 검증한 X-User-Id가 존재하면 해당 사용자 UUID를 사용하고,
 * 인증 주체가 없는 시스템 작업에서는 SYSTEM_USER_ID를 사용합니다.</p>
 */
public class CustomAuditorAware implements AuditorAware<UUID> {

    /**
     * 인증 사용자가 없는 내부 시스템 작업에 사용하는 감사 ID입니다.
     */
    public static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override
    public Optional<UUID> getCurrentAuditor() {

        // 현재 HTTP 요청을 가져옴
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        /*
         * Kafka Consumer, Scheduler, Batch 등
         * HTTP 요청 컨텍스트가 없는 시스템 작업
         * Kafka > UserApprovedEvent > UserService 가 생기면 필요해질 수 있습니다.
         */
        if (attributes == null) {
            return Optional.of(SYSTEM_USER_ID);
        }

        HttpServletRequest request = attributes.getRequest();

        String userId = request.getHeader(HeaderConstants.USER_ID);

        /*
         * Auth → User 회원가입 등 인증 주체가 없는 내부 시스템 요청은
         * SYSTEM_USER_ID를 감사 사용자로 사용합니다.
         *
         * TODO(security):
         * 운영 인프라 구성 시 내부 서비스는 외부에서 직접 접근할 수 없도록
         * Gateway 경유 및 서비스 간 내부 네트워크 정책을 적용합니다.
         */
        if (userId == null || userId.isBlank()) {
            return Optional.of(SYSTEM_USER_ID);
        }

        try {
            return Optional.of(
                    UUID.fromString(userId)
            );
        } catch (IllegalArgumentException exception) {
            /*
             * X-User-Id가 존재한다면 정상적인 UUID여야 합니다.
             * 잘못된 값을 SYSTEM_USER_ID로 대체하면
             * 감사 정보가 왜곡될 수 있으므로 예외로 처리합니다.
             */
            throw new IllegalArgumentException(
                    "유효하지 않은 X-User-Id Header입니다.",
                    exception
            );
        }
    }
}