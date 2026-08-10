package com.sixro.logistics.user.infrastructure.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixro.logistics.user.application.event.UserEventPublisher;
import com.sixro.logistics.user.domain.event.*;
import com.sixro.logistics.user.domain.outbox.OutboxEvent;
import com.sixro.logistics.user.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 상태 변경 이벤트를 Outbox 테이블에 저장합니다.
 *
 * <p>UserCommandService의 트랜잭션 안에서 호출되어
 * 사용자 상태 변경과 Outbox 이벤트 저장이
 * 동일한 DB Transaction으로 처리되도록 합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class OutboxUserEventPublisher
        implements UserEventPublisher {

    private static final String AGGREGATE_TYPE = "USER";

    private static final String USER_CREATED =
            "USER_CREATED";

    private static final String USER_APPROVED =
            "USER_APPROVED";

    private static final String USER_REJECTED =
            "USER_REJECTED";

    private static final String USER_DEACTIVATED =
            "USER_DEACTIVATED";

    private static final String USER_ROLE_CHANGED =
            "USER_ROLE_CHANGED";

    private static final String USER_AFFILIATION_CHANGED =
            "USER_AFFILIATION_CHANGED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(UserCreatedEvent event) {
        save(
                event.userId(),
                USER_CREATED,
                event
        );
    }

    @Override
    public void publish(UserApprovedEvent event) {
        save(
                event.userId(),
                USER_APPROVED,
                event
        );
    }

    @Override
    public void publish(UserRejectedEvent event) {
        save(
                event.userId(),
                USER_REJECTED,
                event
        );
    }

    @Override
    public void publish(UserDeactivatedEvent event) {
        save(
                event.userId(),
                USER_DEACTIVATED,
                event
        );
    }

    @Override
    public void publish(UserRoleChangedEvent event) {
        save(
                event.userId(),
                USER_ROLE_CHANGED,
                event
        );
    }

    @Override
    public void publish(UserAffiliationChangedEvent event) {
        save(
                event.userId(),
                USER_AFFILIATION_CHANGED,
                event
        );
    }

    private void save(
            java.util.UUID aggregateId,
            String eventType,
            Object event
    ) {
        String payload = serialize(event);

        OutboxEvent outboxEvent =
                OutboxEvent.create(
                        aggregateId,
                        AGGREGATE_TYPE,
                        eventType,
                        payload
                );

        outboxEventRepository.save(outboxEvent);
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "사용자 이벤트를 Outbox Payload로 직렬화하지 못했습니다.",
                    exception
            );
        }
    }
}