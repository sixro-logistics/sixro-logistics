package com.sixro.logistics.order.infrastructure.client.company;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", path = "/api/v1/internal/companies")
public interface CompanyClient {

    @GetMapping("/{companyId}")
    CompanyClientResponse getCompany(@PathVariable UUID companyId);

}
