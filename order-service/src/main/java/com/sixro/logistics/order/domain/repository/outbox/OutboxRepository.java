package com.sixro.logistics.order.domain.repository.outbox;

import com.sixro.logistics.order.domain.entity.outbox.Outbox;
import com.sixro.logistics.order.domain.entity.outbox.OutboxStatus;

import java.util.List;

public interface OutboxRepository {

    Outbox save(Outbox outbox);

    List<Outbox> findPendingOutboxes();

}