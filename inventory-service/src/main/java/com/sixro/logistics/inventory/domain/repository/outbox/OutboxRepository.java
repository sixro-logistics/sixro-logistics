package com.sixro.logistics.inventory.domain.repository.outbox;

import com.sixro.logistics.inventory.domain.entity.outbox.Outbox;

import java.util.List;

public interface OutboxRepository {

    Outbox save(Outbox outbox);

    List<Outbox> findPendingOutboxes();

}