package com.sixro.logistics.order.application.service.event;

import com.sixro.logistics.order.domain.entity.event.ProcessedEvent;
import com.sixro.logistics.order.domain.repository.event.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    public void save(UUID eventId) {
        processedEventRepository.save(ProcessedEvent.create(eventId));
    }
}
