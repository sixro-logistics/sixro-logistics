package com.sixro.logistics.company.presentation.dto.response.internal;

import com.sixro.logistics.company.domain.entity.Company;

import java.util.UUID;

public record CompanyInternalCheckResponseDto(
        UUID companyId,
        String companyName
) {
    public static CompanyInternalCheckResponseDto from(Company company) {
        return new CompanyInternalCheckResponseDto(
                company.getCompanyId(),
                company.getCompanyName()
        );
    }
}
