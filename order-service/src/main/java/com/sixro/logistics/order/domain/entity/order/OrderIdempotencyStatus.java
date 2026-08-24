package com.sixro.logistics.order.domain.entity.order;

public enum OrderIdempotencyStatus {

    // 주문 요청 처리 중
    PROCESSING,

    // 주문 생성 성공
    SUCCEEDED,

    // 주문 실패 + 재고 복원 완료
    COMPENSATED
}