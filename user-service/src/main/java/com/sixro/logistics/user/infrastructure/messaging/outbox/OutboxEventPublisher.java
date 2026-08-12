package com.sixro.logistics.user.infrastructure.messaging.outbox;

import com.sixro.logistics.user.domain.outbox.OutboxEvent;
import com.sixro.logistics.user.domain.outbox.OutboxEventRepository;
import com.sixro.logistics.user.infrastructure.messaging.kafka.KafkaUserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PENDING 상태의 Outbox 이벤트를 조회하여
 * Kafka로 발행하는 Polling Publisher입니다.
 *
 * <p>Kafka 발행에 성공한 이벤트는 PUBLISHED 상태로 변경하고,
 * 발행에 실패한 이벤트는 PENDING 상태로 유지하여
 * 다음 Polling 주기에 다시 시도합니다.</p>
 *
 *  * 현재 Consumer 구현 현황
 *  *
 *  * - user-created:
 *  *   현재 Consumer 미구현
 *  *   TODO 향후 가입 신청 알림 등의 기능에서 확장 예정
 *  *
 *  * - user-approved:
 *  *   현재 Consumer 미구현
 *  *   TODO 향후 승인 알림 또는 배송 담당자 연동 기능에서 확장 예정
 *  *
 *  * - user-rejected:
 *  *   현재 Consumer 미구현
 *  *   TODO 향후 가입 거절 알림 등의 기능에서 확장 예정
 *  *
 *  * - user-deactivated:
 *  *   Auth Service에서 소비하여 Refresh Token과 Session을 무효화
 *  *
 *  * - user-role-changed:
 *  *   Auth Service에서 소비하여 Refresh Token과 Session을 무효화
 *  *
 *  * - user-affiliation-changed:
 *  *   Auth Service에서 소비하여 Refresh Token과 Session을 무효화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final String USER_CREATED_TOPIC =
            "user-created";

    private static final String USER_APPROVED_TOPIC =
            "user-approved";

    private static final String USER_REJECTED_TOPIC =
            "user-rejected";

    private static final String USER_DEACTIVATED_TOPIC =
            "user-deactivated";

    private static final String USER_ROLE_CHANGED_TOPIC =
            "user-role-changed";

    private static final String USER_AFFILIATION_CHANGED_TOPIC =
            "user-affiliation-changed";

    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaUserEventPublisher kafkaUserEventPublisher;

    /**
     * 일정 주기로 미발행 Outbox 이벤트를 조회하고 Kafka로 발행합니다.
     *
     * <p>이 메서드의 Transaction이 유지되는 동안 조회한 Outbox 행의
     * 배타적 잠금도 유지됩니다. 따라서 {@code @Transactional}을
     * 제거하면 안 됩니다.</p>
     */
    @Scheduled(
            fixedDelayString =
                    "${outbox.publisher.fixed-delay:1000}"
    )
    @Transactional
    public void publishPendingEvents() {

        /*
         * 다른 User Service 인스턴스가 이미 선점한 이벤트는
         * FOR UPDATE SKIP LOCKED에 의해 조회되지 않습니다.
         */
        List<OutboxEvent> pendingEvents =
                outboxEventRepository
                        .findPendingEventsForUpdate();

        for (OutboxEvent outboxEvent : pendingEvents) {
            publish(outboxEvent);
        }
    }

    /**
     * 단일 Outbox 이벤트를 Kafka로 발행합니다.
     */
    private void publish(OutboxEvent outboxEvent) {

        try {
            String topic =
                    resolveTopic(outboxEvent.getEventType());

            /*
             * Consumer가 중복 이벤트를 구분할 수 있도록
             * Outbox eventId와 eventType을 Kafka Header에 포함합니다.
             *
             * send()는 비동기로 수행되므로 완료 결과를 확인한 뒤
             * Outbox 상태를 PUBLISHED로 변경합니다.
             */
            kafkaUserEventPublisher.publish(
                    topic,
                    outboxEvent
                            .getAggregateId()
                            .toString(),
                    outboxEvent.getPayload(),
                    outboxEvent.getEventId(),
                    outboxEvent.getEventType()
            ).join();

            outboxEvent.markPublished();

            /*
             * 발행 성공 상태를 DB에 반영합니다.
             */
            outboxEventRepository.save(outboxEvent);

            log.info(
                    "Outbox 이벤트 Kafka 발행 완료. "
                            + "eventId={}, eventType={}, topic={}",
                    outboxEvent.getEventId(),
                    outboxEvent.getEventType(),
                    topic
            );

        } catch (Exception exception) {
            handlePublishFailure(
                    outboxEvent,
                    exception
            );
        }
    }

    /**
     * Kafka 발행 실패 시 재시도 횟수와 Outbox 상태를 변경합니다.
     */
    private void handlePublishFailure(
            OutboxEvent outboxEvent,
            Exception exception
    ) {
        outboxEvent.increaseRetryCount();

        if (outboxEvent.getRetryCount()
                >= MAX_RETRY_COUNT) {

            outboxEvent.markFailed();

            outboxEventRepository.save(
                    outboxEvent
            );

            log.error(
                    "Outbox 이벤트 최종 발행 실패. "
                            + "eventId={}, eventType={}, retryCount={}",
                    outboxEvent.getEventId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getRetryCount(),
                    exception
            );

            return;
        }

        /*
         * 다음 Polling 주기에 다시 조회될 수 있도록
         * PENDING 상태를 유지하고 재시도 횟수만 저장합니다.
         */
        outboxEventRepository.save(
                outboxEvent
        );

        log.warn(
                "Outbox 이벤트 Kafka 발행 실패. 재시도 예정. "
                        + "eventId={}, eventType={}, retryCount={}",
                outboxEvent.getEventId(),
                outboxEvent.getEventType(),
                outboxEvent.getRetryCount(),
                exception
        );
    }

    /**
     * Outbox 이벤트 유형에 대응하는 Kafka Topic을 반환합니다.
     */
    private String resolveTopic(String eventType) {

        return switch (eventType) {

            case "USER_CREATED" ->
                    USER_CREATED_TOPIC;

            case "USER_APPROVED" ->
                    USER_APPROVED_TOPIC;

            case "USER_REJECTED" ->
                    USER_REJECTED_TOPIC;

            case "USER_DEACTIVATED" ->
                    USER_DEACTIVATED_TOPIC;

            case "USER_ROLE_CHANGED" ->
                    USER_ROLE_CHANGED_TOPIC;

            case "USER_AFFILIATION_CHANGED" ->
                    USER_AFFILIATION_CHANGED_TOPIC;

            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 Outbox 이벤트 유형입니다: "
                                    + eventType
                    );
        };
    }
}