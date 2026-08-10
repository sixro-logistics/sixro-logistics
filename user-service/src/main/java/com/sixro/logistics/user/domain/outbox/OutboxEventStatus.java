package com.sixro.logistics.user.domain.outbox;

/**
 * Outbox 이벤트의 처리 상태를 나타냅니다.
 */
public enum OutboxEventStatus {

    /**
     * 아직 외부 메시징 시스템으로 발행되지 않은 상태입니다.
     */
    PENDING,

    /**
     * Kafka 발행이 완료된 상태입니다.
     */
    PUBLISHED,

    /**
     * 재시도 정책을 모두 소진하여 최종적으로 발행에 실패한 상태입니다.
     */
    FAILED
}