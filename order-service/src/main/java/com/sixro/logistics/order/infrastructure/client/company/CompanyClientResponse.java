package com.sixro.logistics.order.infrastructure.client.company;

import java.util.UUID;

public record CompanyClientResponse(
        UUID companyId,
        String address,
        UUID hubId
) {
}
