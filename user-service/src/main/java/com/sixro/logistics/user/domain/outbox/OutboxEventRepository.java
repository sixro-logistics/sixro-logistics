package com.sixro.logistics.user.domain.outbox;

import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    List<OutboxEvent> findPendingEvents();
}