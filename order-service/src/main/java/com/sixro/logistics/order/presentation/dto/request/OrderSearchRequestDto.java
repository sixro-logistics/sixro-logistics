package com.sixro.logistics.order.presentation.dto.request;

import com.sixro.logistics.order.application.command.OrderSearchCommand;

import java.util.UUID;

public record OrderSearchRequestDto(
        UUID hubId,
        UUID receiverCompanyId,
        UUID supplierCompanyId
) {

    public OrderSearchCommand toCommand(){
        return new OrderSearchCommand(hubId, receiverCompanyId, supplierCompanyId);
    }

}
