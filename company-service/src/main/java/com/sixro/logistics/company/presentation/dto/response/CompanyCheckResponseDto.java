package com.sixro.logistics.company.presentation.dto.response;

import com.sixro.logistics.company.domain.entity.Company;

import java.util.UUID;

public record CompanyCheckResponseDto(
        UUID companyId,
        String companyName,
        Boolean isDeleted
) {
    public static CompanyCheckResponseDto from(Company company) {
        return new CompanyCheckResponseDto(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getIsDeleted()
        );
    }
}
