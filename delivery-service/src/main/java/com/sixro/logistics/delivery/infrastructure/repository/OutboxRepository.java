package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;
import com.sixro.logistics.delivery.domain.entity.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    List<Outbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
