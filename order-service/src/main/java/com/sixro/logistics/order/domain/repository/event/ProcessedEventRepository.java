package com.sixro.logistics.order.domain.repository.event;

import com.sixro.logistics.order.domain.entity.event.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository {
    boolean existsByEventId(UUID eventId);

    void save(ProcessedEvent processedEvent);
}