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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final String USER_APPROVED_TOPIC =
            "user-approved";

    private static final String USER_REJECTED_TOPIC =
            "user-rejected";

    private static final String USER_DEACTIVATED_TOPIC =
            "user-deactivated";

    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaUserEventPublisher kafkaUserEventPublisher;

    /**
     * 일정 주기로 미발행 Outbox 이벤트를 처리합니다.
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:1000}")
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findPendingEvents();

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
             * Kafka send()는 비동기로 수행되므로
             * 완료 결과를 확인한 후 PUBLISHED 처리합니다.
             */
            kafkaUserEventPublisher.publish(
                    topic,
                    outboxEvent.getAggregateId().toString(),
                    outboxEvent.getPayload()
            ).join();

            outboxEvent.markPublished();

            /*
             * 발행 성공 상태를 DB에 반영합니다.
             */
            outboxEventRepository.save(outboxEvent);

            log.info(
                    "Outbox 이벤트 Kafka 발행 완료. eventId={}, eventType={}",
                    outboxEvent.getEventId(),
                    outboxEvent.getEventType()
            );

        } catch (Exception exception) {

            outboxEvent.increaseRetryCount();

            if (outboxEvent.getRetryCount() >= MAX_RETRY_COUNT) {
                outboxEvent.markFailed();

                log.error(
                        "Outbox 이벤트 최종 발행 실패. eventId={}, eventType={}, retryCount={}",
                        outboxEvent.getEventId(),
                        outboxEvent.getEventType(),
                        outboxEvent.getRetryCount(),
                        exception
                );

                return;
            }

            log.warn(
                    "Outbox 이벤트 Kafka 발행 실패. 재시도 예정. eventId={}, eventType={}, retryCount={}",
                    outboxEvent.getEventId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getRetryCount(),
                    exception
            );
        }
    }

    /**
     * Outbox 이벤트 유형에 대응하는 Kafka Topic을 반환합니다.
     */
    private String resolveTopic(String eventType) {

        return switch (eventType) {
            case "USER_APPROVED" ->
                    USER_APPROVED_TOPIC;

            case "USER_REJECTED" ->
                    USER_REJECTED_TOPIC;

            case "USER_DEACTIVATED" ->
                    USER_DEACTIVATED_TOPIC;

            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 Outbox 이벤트 유형입니다: "
                                    + eventType
                    );
        };
    }
}