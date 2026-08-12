package com.sixro.logistics.order.presentation.dto.request;

import com.sixro.logistics.order.application.command.OrderCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequestDto(

        @NotNull(message = "허브 id는 필수입니다.")
        UUID hubId,

        @NotNull(message = "수령인 id는 필수입니다.")
        UUID receiverId,

        @NotNull(message = "수령 업체 id는 필수입니다.")
        UUID receiverCompanyId,

        @NotNull(message = "납품 기한은 필수입니다.")
        @Future(message = "납품 기한은 미래 시각이어야 합니다.")
        LocalDateTime deliveryDeadline,

        @Size(max = 255, message = "요청사항은 255자를 넘을 수 없습니다.")
        String requests,

        @NotEmpty(message = "주문 상품은 하나 이상 입력해야 합니다.")
        @Valid
        List<OrderRequestItemDto> orderItems

) {

        public OrderCreateCommand toCommand() {
                return new OrderCreateCommand(
                        hubId,
                        receiverId,
                        receiverCompanyId,
                        deliveryDeadline,
                        requests,
                        orderItems.stream()
                                .map(OrderRequestItemDto::toCommand)
                                .toList()
                );
        }

}
