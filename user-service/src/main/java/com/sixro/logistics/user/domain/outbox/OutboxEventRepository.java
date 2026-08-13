package com.sixro.logistics.user.domain.outbox;

import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    /**
     * Kafka로 발행할 PENDING Outbox 이벤트를 배타적으로 선점합니다.
     *
     * <p>Infrastructure 구현체에서는 여러 User Service 인스턴스가
     * 동일한 이벤트를 동시에 발행하지 않도록 행 잠금을 적용합니다.</p>
     */
    List<OutboxEvent> findPendingEventsForUpdate();
}