package com.sixro.logistics.order.application.command;

import java.util.UUID;

public record OrderSearchCommand(
        UUID hubId,
        UUID receiverCompanyId,
        UUID supplierCompanyId
) {
}
