package com.sixro.logistics.delivery.infrastructure.client.company;

import java.util.UUID;

public record CompanyClientResponse(
        UUID destCompanyId,
        UUID destHubId
) {
}
