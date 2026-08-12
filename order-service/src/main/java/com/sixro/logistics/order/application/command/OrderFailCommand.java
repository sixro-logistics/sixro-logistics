package com.sixro.logistics.order.application.command;

import com.sixro.logistics.common.core.exception.ErrorCode;

import java.util.UUID;

public record OrderFailCommand(
        UUID orderId
) {
}
