package com.sixro.logistics.delivery.infrastructure.repository.adapter;

import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import com.sixro.logistics.delivery.domain.port.OutboxRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.repository.OutboxRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final OutboxRepository outboxRepository;

    public OutboxRepositoryAdapter(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public Outbox save(Outbox outbox) {
        return outboxRepository.save(outbox);
    }

    @Override
    public List<Outbox> findPendingOutboxes() {
        return outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }
}
