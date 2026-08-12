package com.sixro.logistics.order.presentation.dto.request;

import com.sixro.logistics.order.application.command.OrderCommandItem;
import com.sixro.logistics.order.application.command.OrderUpdateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record OrderUpdateRequestDto(

        @Future(message = "납품 기한은 미래 시각이어야 합니다.")
        LocalDateTime deliveryDeadline,

        @Size(max = 255, message = "요청사항은 255자를 넘을 수 없습니다.")
        String requests
) {

    public OrderUpdateCommand toCommand() {
        return new OrderUpdateCommand(
                deliveryDeadline,
                requests
        );
    }

}
