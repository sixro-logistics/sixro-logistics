package com.sixro.logistics.user.domain.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Service의 도메인 변경과 함께 저장되는
 * Transactional Outbox 이벤트입니다.
 *
 * <p>도메인 데이터 변경과 Outbox 이벤트 저장을
 * 동일한 DB Transaction으로 처리하여
 * DB 변경과 이벤트 발행 요청 사이의 불일치를 줄입니다.</p>
 */
@Entity
@Table(name = "p_user_outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    /**
     * 이벤트가 발생한 Aggregate의 식별자입니다.
     *
     * <p>User 이벤트에서는 userId가 저장됩니다.</p>
     */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /**
     * 이벤트가 발생한 Aggregate 종류입니다.
     *
     * <p>현재 User Service에서는 USER를 사용합니다.</p>
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /**
     * 발행할 이벤트 종류입니다.
     *
     * <p>예: USER_APPROVED, USER_REJECTED, USER_DEACTIVATED</p>
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * 외부 메시징 시스템으로 전달할 이벤트 Payload입니다.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    private OutboxEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 새로운 Outbox 이벤트를 생성합니다.
     */
    public static OutboxEvent create(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload
    ) {
        return new OutboxEvent(
                aggregateId,
                aggregateType,
                eventType,
                payload
        );
    }

    /**
     * Kafka 발행 완료 상태로 변경합니다.
     */
    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * Kafka 실패 처리에 대해 재시도 회수를 정의합니다.
     */
    public void increaseRetryCount() {
        this.retryCount++;
    }

    public void markFailed() {
        this.status = OutboxEventStatus.FAILED;
    }

}