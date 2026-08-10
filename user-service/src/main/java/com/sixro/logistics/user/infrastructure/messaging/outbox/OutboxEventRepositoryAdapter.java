package com.sixro.logistics.user.infrastructure.messaging.outbox;

import com.sixro.logistics.user.domain.outbox.OutboxEvent;
import com.sixro.logistics.user.domain.outbox.OutboxEventStatus;
import com.sixro.logistics.user.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter
        implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    @Override
    public List<OutboxEvent> findPendingEvents() {
        return outboxEventJpaRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING
                );
    }
}