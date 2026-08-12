package com.sixro.logistics.inventory.domain.repository.event;

import com.sixro.logistics.inventory.domain.event.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository {
    boolean existsByEventId(UUID eventId);

    void save(ProcessedEvent processedEvent);
}