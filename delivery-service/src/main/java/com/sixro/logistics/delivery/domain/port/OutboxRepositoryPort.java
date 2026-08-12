package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.entity.outbox.Outbox;

import java.util.List;

public interface OutboxRepositoryPort {

    Outbox save(Outbox outbox);

    List<Outbox> findPendingOutboxes();
}
