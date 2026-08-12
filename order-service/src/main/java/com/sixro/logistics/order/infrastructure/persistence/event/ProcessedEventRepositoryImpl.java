package com.sixro.logistics.order.infrastructure.persistence.event;

import com.sixro.logistics.order.domain.entity.event.ProcessedEvent;
import com.sixro.logistics.order.domain.repository.event.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public void save(ProcessedEvent processedEvent) {
        jpaRepository.save(processedEvent);
    }
}
