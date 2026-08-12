package com.sixro.logistics.company.presentation.dto.response.internal;

import com.sixro.logistics.company.domain.entity.Company;

import java.util.UUID;

public record CompanyInternalHubInfoResponseDto(
        UUID destCompanyId,
        UUID destHubId
) {
    public static CompanyInternalHubInfoResponseDto from(Company company) {
        return new CompanyInternalHubInfoResponseDto(
                company.getCompanyId(),
                company.getHubId()
        );
    }
}
