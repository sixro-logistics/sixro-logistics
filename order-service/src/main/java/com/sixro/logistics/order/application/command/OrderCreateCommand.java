package com.sixro.logistics.order.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateCommand(

        // 주문 생성 요청을 식별하기 위한 멱등키
        // 최초 요청부터 외부에서 전달되며, 동일한 요청의 재시도 시에도 같은 값을 사용
        UUID idempotencyKey,

        UUID hubId,
        UUID receiverId,
        UUID receiverCompanyId,
        LocalDateTime deliveryDeadline,
        String requests,
        List<OrderCommandItem> orderItems

) {
}
