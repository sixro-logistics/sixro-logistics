package com.sixro.logistics.delivery.domain.entity.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "delivery_schema", name = "p_outbox",
        indexes = @Index(name = "idx_outbox_status_created_at", columnList = "status, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {

    @Id
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    // 이벤트 대상 ID: 배송생성은 deliveryId, 배송생성실패는 orderId
    @Column(name = "aggregate_id", updatable = false, nullable = false)
    private UUID aggregateId;

    // DELIVERY_CREATED, DELIVERY_CREATION_FAILED
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", updatable = false, nullable = false)
    private OutboxEventType eventType;

    // Kafka Header 재구성용
    @Column(name = "trace_id", updatable = false, nullable = false)
    private String traceId;

    @Column(updatable = false, nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    // outbox 생성시각
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // Kafka 발행성공시각
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private Outbox(UUID eventId, UUID aggregateId, OutboxEventType eventType, String traceId, String payload, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.traceId = traceId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = createdAt;
    }

    public static Outbox create(
            UUID eventId, UUID aggregateId, OutboxEventType eventType,
            String traceId, String payload, LocalDateTime createdAt) {

        return new Outbox(eventId, aggregateId, eventType, traceId, payload, createdAt);
    }

    public void publish(LocalDateTime publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }
}
