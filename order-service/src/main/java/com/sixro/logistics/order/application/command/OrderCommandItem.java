package com.sixro.logistics.order.application.command;

import java.util.UUID;

public record OrderCommandItem(
        UUID productId,
        Integer quantity
) {

}
