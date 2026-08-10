package com.sixro.logistics.inventory.infrastructure.persistence.outbox;

import com.sixro.logistics.inventory.domain.entity.outbox.Outbox;
import com.sixro.logistics.inventory.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.inventory.domain.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxJpaRepository outboxJpaRepository;

    @Override
    public Outbox save(Outbox outbox) {
        return outboxJpaRepository.save(outbox);
    }

    @Override
    public List<Outbox> findPendingOutboxes() {
        return outboxJpaRepository.findByStatus(OutboxStatus.PENDING);
    }
}