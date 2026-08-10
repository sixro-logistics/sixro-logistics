package com.sixro.logistics.inventory.infrastructure.persistence.outbox;

import com.sixro.logistics.inventory.domain.entity.outbox.Outbox;
import com.sixro.logistics.inventory.domain.entity.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<Outbox, UUID> {

    List<Outbox> findByStatus(OutboxStatus status);

}