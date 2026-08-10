package com.sixro.logistics.order.application.command;

import java.util.UUID;

public record OrderCreateServiceItem(

        UUID productId,
        String productName,
        Integer productPrice,
        UUID companyId,
        Integer quantity

) {
}