package com.sixro.logistics.user.infrastructure.messaging.outbox;

import com.sixro.logistics.user.domain.outbox.OutboxEvent;
import com.sixro.logistics.user.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter
        implements OutboxEventRepository {

    private final OutboxEventJpaRepository
            outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    /**
     * 현재 Transaction에서 처리할 PENDING 이벤트를 선점합니다.
     *
     * <p>다른 User Service 인스턴스가 이미 선점한 행은
     * {@code SKIP LOCKED}에 의해 조회 대상에서 제외됩니다.</p>
     */
    @Override
    public List<OutboxEvent> findPendingEventsForUpdate() {
        return outboxEventJpaRepository
                .findPendingEventsForUpdate();
    }
}