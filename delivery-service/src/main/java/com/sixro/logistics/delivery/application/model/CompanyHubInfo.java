package com.sixro.logistics.delivery.application.model;

import java.util.UUID;

// Application이 이해하는 수령 업체의 목적지 허브 정보
public record CompanyHubInfo(
        UUID destCompanyId,
        UUID destHubId
) {
}
