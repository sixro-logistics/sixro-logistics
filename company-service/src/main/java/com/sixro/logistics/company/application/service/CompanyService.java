package com.sixro.logistics.company.application.service;

import com.sixro.logistics.company.presentation.dto.request.CompanyCreateRequestDto;
import com.sixro.logistics.company.presentation.dto.response.CompanyResponseDto;
import com.sixro.logistics.company.presentation.dto.request.CompanyUpdateRequestDto;
import com.sixro.logistics.company.domain.entity.Company;
import com.sixro.logistics.company.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * 업체 목록 조회
     */
    public Page<CompanyResponseDto> getCompanies(
            UUID hubId,
            String companyName,
            String companyType,
            Pageable pageable
    ) {

        Specification<Company> specification = isNotDeleted();

        if (hubId != null) {
            specification = specification.and(hasHubId(hubId));
        }

        if (companyName != null && !companyName.isBlank()) {
            specification = specification.and(
                    hasCompanyName(companyName)
            );
        }

        if (companyType != null && !companyType.isBlank()) {
            specification = specification.and(
                    hasCompanyType(companyType)
            );
        }

        return companyRepository
                .findAll(specification, pageable)
                .map(CompanyResponseDto::from);
    }

    /**
     * 업체 상세 조회
     */
    public CompanyResponseDto getCompany(UUID companyId) {

        Company company = companyRepository
                .findByCompanyIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 업체입니다.")
                );

        return CompanyResponseDto.from(company);
    }

    /**
     * 업체 등록
     */
    @Transactional
    public CompanyResponseDto createCompany(
            CompanyCreateRequestDto request,
            UUID userId
    ) {

        if (companyRepository
                .existsByBusinessNumberAndIsDeletedFalse(
                        request.getBusinessNumber()
                )) {

            throw new IllegalArgumentException(
                    "이미 등록된 사업자번호입니다."
            );
        }

        Company company = Company.builder()
                .hubId(request.getHubId())
                .companyName(request.getCompanyName())
                .companyType(request.getCompanyType())
                .businessNumber(request.getBusinessNumber())
                .zipcode(request.getZipcode())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .createdBy(userId)
                .updatedBy(userId)
                .isDeleted(false)
                .build();

        Company savedCompany = companyRepository.save(company);

        return CompanyResponseDto.from(savedCompany);
    }

    /**
     * 업체 수정
     */
    @Transactional
    public CompanyResponseDto updateCompany(
            UUID companyId,
            CompanyUpdateRequestDto request,
            UUID userId
    ) {

        Company company = companyRepository
                .findByCompanyIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 업체입니다.")
                );

        company.update(
                request.getHubId(),
                request.getCompanyName(),
                request.getCompanyType(),
                request.getBusinessNumber(),
                request.getZipcode(),
                request.getAddress(),
                request.getDetailAddress(),
                request.getContactName(),
                request.getContactEmail(),
                request.getContactPhone(),
                userId
        );

        return CompanyResponseDto.from(company);
    }

    /**
     * 업체 논리 삭제
     */
    @Transactional
    public void deleteCompany(
            UUID companyId,
            UUID userId
    ) {

        Company company = companyRepository
                .findByCompanyIdAndIsDeletedFalse(companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 업체입니다.")
                );

        company.delete(userId);
    }

    private Specification<Company> isNotDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("isDeleted"));
    }

    private Specification<Company> hasHubId(UUID hubId) {
        return (root, query, cb) ->
                cb.equal(root.get("hubId"), hubId);
    }

    private Specification<Company> hasCompanyName(String companyName) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("companyName")),
                        "%" + companyName.toLowerCase() + "%"
                );
    }

    private Specification<Company> hasCompanyType(String companyType) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("companyType"),
                        companyType.toUpperCase()
                );
    }
}
