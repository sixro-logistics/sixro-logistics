package com.sixro.logistics.user.infrastructure.messaging.outbox;

import com.sixro.logistics.user.domain.outbox.OutboxEvent;
import com.sixro.logistics.user.domain.outbox.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * OutboxEvent의 JPA 저장소입니다.
 *
 * <p>Infrastructure 계층에서 Spring Data JPA를 사용해
 * Outbox 이벤트의 저장 및 조회를 담당합니다.</p>
 */
public interface OutboxEventJpaRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxEventStatus status
    );
}