package com.sixro.logistics.delivery.application.port;

import com.sixro.logistics.delivery.application.model.CompanyHubInfo;

import java.util.Optional;
import java.util.UUID;

// app -> infra 요구 기능 정의
public interface CompanyQueryPort {

    Optional<CompanyHubInfo> findHubInfo(UUID companyId);
}
