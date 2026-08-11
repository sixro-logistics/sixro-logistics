package com.sixro.logistics.company.presentation.dto.response.internal;

import com.sixro.logistics.company.domain.entity.Company;

import java.util.UUID;

public record CompanyInternalCheckResponseDto(
        UUID companyId,
        UUID hubId,
        String companyName,
        String address
) {
    public static CompanyInternalCheckResponseDto from(Company company) {
        return new CompanyInternalCheckResponseDto(
                company.getCompanyId(),
                company.getHubId(),
                company.getCompanyName(),
                company.getAddress()
        );
    }
}
