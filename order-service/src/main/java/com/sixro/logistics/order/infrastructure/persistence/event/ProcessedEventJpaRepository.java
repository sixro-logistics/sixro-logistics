package com.sixro.logistics.order.infrastructure.persistence.event;

import com.sixro.logistics.order.domain.entity.event.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);
}