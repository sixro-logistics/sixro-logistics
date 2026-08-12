package com.sixro.logistics.order.application.result;

import org.springframework.data.domain.Page;

public record OrderSearchResult(
    Page<OrderSearchItem> page
) {
}
