package com.sixro.logistics.order.domain.entity.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "p_order_processed_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_processed_event_event_id",
                        columnNames = "event_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    private ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public static ProcessedEvent create(UUID eventId) {
        return new ProcessedEvent(eventId);
    }
}