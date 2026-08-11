package com.sixro.logistics.company.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.company.domain.entity.Company;
import com.sixro.logistics.company.domain.repository.CompanyRepository;
import com.sixro.logistics.company.exception.CompanyErrorCode;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalCheckResponseDto;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalHubInfoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyInternalService {

    private final CompanyRepository companyRepository;

    /**
     * 업체 존재 유무 확인
     */
    public CompanyInternalCheckResponseDto getCheckCompany(UUID companyId) {
        Company company = companyRepository
                .findByCompanyIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyInternalCheckResponseDto.from(company);
    }

    /**
     * 목적지 허브를 반환
     */
    public CompanyInternalHubInfoResponseDto getCompanyHubInfo(UUID companyId) {
        Company company = companyRepository
                .findByCompanyIdAndIsDeletedFalse(companyId)
                .orElseThrow(() -> new BaseException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyInternalHubInfoResponseDto.from(company);
    }
}
