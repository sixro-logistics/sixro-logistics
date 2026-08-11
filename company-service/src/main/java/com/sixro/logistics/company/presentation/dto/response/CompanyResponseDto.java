package com.sixro.logistics.company.presentation.dto.response;

import com.sixro.logistics.company.domain.entity.Company;
import com.sixro.logistics.company.domain.entity.CompanyType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CompanyResponseDto {

    private UUID companyId;
    private UUID hubId;
    private String companyName;
    private CompanyType companyType;
    private String businessNumber;
    private String zipcode;
    private String address;
    private String detailAddress;
    private String contactName;
    private String contactEmail;
    private String contactPhone;

    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;

    public static CompanyResponseDto from(Company company) {
        return CompanyResponseDto.builder()
                .companyId(company.getCompanyId())
                .hubId(company.getHubId())
                .companyName(company.getCompanyName())
                .companyType(company.getCompanyType())
                .businessNumber(company.getBusinessNumber())
                .zipcode(company.getZipcode())
                .address(company.getAddress())
                .detailAddress(company.getDetailAddress())
                .contactName(company.getContactName())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .createdAt(company.getCreatedAt())
                .createdBy(company.getCreatedBy())
                .updatedAt(company.getUpdatedAt())
                .updatedBy(company.getUpdatedBy())
                .build();
    }
}
