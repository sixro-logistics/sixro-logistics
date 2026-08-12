package com.sixro.logistics.delivery.infrastructure.client.company;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "company-service", path = "/api/v1/internal/companies")
public interface CompanyClient {

    @GetMapping("/hub-info")
    CommonResponse<CompanyClientResponse> getHubInfo(@RequestParam("companyId") UUID companyId);
}
