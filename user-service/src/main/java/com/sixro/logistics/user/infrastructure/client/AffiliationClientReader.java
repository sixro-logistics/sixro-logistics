package com.sixro.logistics.user.infrastructure.client;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.infrastructure.client.response.InternalCompanyResponse;
import com.sixro.logistics.user.infrastructure.client.response.InternalHubResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 소속 검증을 위한 내부 서비스 호출에 Circuit Breaker를 적용합니다.
 */
@Component
@RequiredArgsConstructor
public class AffiliationClientReader {

    private final HubServiceClient hubServiceClient;
    private final CompanyServiceClient companyServiceClient;

    @CircuitBreaker(name = "hubAffiliation")
    public CommonResponse<InternalHubResponse> getHub(UUID hubId) {
        return hubServiceClient.getHub(hubId);
    }

    @CircuitBreaker(name = "companyAffiliation")
    public CommonResponse<InternalCompanyResponse> getCompany(
            UUID companyId
    ) {
        return companyServiceClient.getCompany(companyId);
    }
}