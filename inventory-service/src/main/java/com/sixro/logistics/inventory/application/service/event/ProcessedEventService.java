package com.sixro.logistics.inventory.application.service.event;

import com.sixro.logistics.inventory.domain.event.ProcessedEvent;
import com.sixro.logistics.inventory.domain.repository.event.ProcessedEventRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void save(UUID eventId) {
        processedEventRepository.save(ProcessedEvent.create(eventId));
    }
}