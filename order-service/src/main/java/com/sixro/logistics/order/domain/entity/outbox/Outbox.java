package com.sixro.logistics.order.domain.entity.outbox;

import com.sixro.logistics.common.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_order_outbox" , schema = "order_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Outbox extends BaseEntity {

    // Outbox의 pk
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outbox_id", updatable = false)
    private UUID id;

    // 주문의 pk
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private AggregateType aggregateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private EventType eventType;

    @Lob
    @Column(nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private Outbox(
            UUID aggregateId,
            AggregateType aggregateType,
            EventType eventType,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
    }

    public static Outbox create(
            UUID aggregateId,
            AggregateType aggregateType,
            EventType eventType,
            String payload
    ) {
        return new Outbox(
                aggregateId,
                aggregateType,
                eventType,
                payload
        );
    }

    public void publish() {
        this.status = OutboxStatus.PUBLISHED;
    }

    public void fail() {
        this.status = OutboxStatus.FAILED;
    }

}