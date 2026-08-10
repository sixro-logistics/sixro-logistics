package com.sixro.logistics.order.application.model;

import java.util.UUID;

public record CompanyInfo(
        UUID companyId,
        String address,
        UUID hubId
) {
}
