package com.sixro.logistics.user.infrastructure.client;


import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.infrastructure.client.config.AffiliationClientConfig;
import com.sixro.logistics.user.infrastructure.client.response.InternalCompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "company-service",
        contextId = "companyAffiliationClient",
        path = "/api/v1/internal/companies",
        configuration = AffiliationClientConfig.class
)
public interface CompanyServiceClient {

    @GetMapping("/{companyId}")
    CommonResponse<InternalCompanyResponse> getCompany(
            @PathVariable("companyId") UUID companyId
    );

}
