package com.sixro.logistics.company.presentation.controller;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.company.application.service.CompanyService;
import com.sixro.logistics.company.presentation.dto.request.CompanyCreateRequestDto;
import com.sixro.logistics.company.presentation.dto.request.CompanyUpdateRequestDto;
import com.sixro.logistics.company.presentation.dto.response.CompanyResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 업체 목록 조회
     * GET /companies
     * GET /companies?companyType=PRODUCER
     * GET /companies?hubId=UUID
     * GET /companies?companyName=삼성
     * GET /companies?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<CompanyResponseDto>>> getCompanies(
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String companyType,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Page<CompanyResponseDto> response =
                companyService.getCompanies(
                        hubId,
                        companyName,
                        companyType,
                        pageable
                );

        return ResponseEntity.ok(CommonResponse.success("업체 목록 조회에 성공했습니다.", response));
    }

    /**
     * 업체 상세 조회
     */
    @GetMapping("/{companyId}")
    public ResponseEntity<CommonResponse<CompanyResponseDto>> getCompany(
            @PathVariable UUID companyId
    ) {
        return ResponseEntity.ok(CommonResponse.success("업체 목록 조회에 성공했습니다.", companyService.getCompany(companyId)));
    }

    /**
     * 업체 등록
     */
    @PostMapping
    public ResponseEntity<CompanyResponseDto> createCompany(
            @Valid @RequestBody CompanyCreateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {
        // 권한 검증: MASTER 또는 HUB_ADMIN 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole) && !"HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 본인 허브에 대한 업체만 등록 가능
            if("HUB_ADMIN".equalsIgnoreCase(userRole)) {
                if (!affiliationId.equals(request.getHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        CompanyResponseDto response =
                companyService.createCompany(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 업체 수정
     */
    @PatchMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDto> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyUpdateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {
        // 권한 검증: MASTER 또는 HUB_MANAGER가 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole) && !"HUB_ADMIN".equalsIgnoreCase(userRole) && !"COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
            if("HUB_ADMIN".equalsIgnoreCase(userRole)) {
                // HUB_ADMIN은 본인 허브에 대한 업체만 수정 가능
                if (!affiliationId.equals(request.getHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            } else if("COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
                // COMPANY_MANAGER는 본인 업체에 대한 업체만 수정 가능
                if (!affiliationId.equals(companyId)) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        CompanyResponseDto response =
                companyService.updateCompany(
                        companyId,
                        request,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 업체 삭제
     */
    @DeleteMapping("/{companyId}")
    public ResponseEntity<CommonResponse<Void>> deleteCompany(
            @PathVariable UUID companyId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-Affiliation-Id") UUID affiliationId
    ) {
        // 권한 검증: MASTER 또는 HUB_MANAGER가 아닌 경우 예외 발생
        if (!"MASTER_ADMIN".equalsIgnoreCase(userRole) && !"HUB_ADMIN".equalsIgnoreCase(userRole)) {
            if("HUB_ADMIN".equalsIgnoreCase(userRole)) {
                CompanyResponseDto resCompanyDto = companyService.getCompany(companyId);
                // HUB_ADMIN은 본인 허브에 대한 업체만 삭제 가능
                if (!affiliationId.equals(resCompanyDto.getHubId())) {
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
                }
            }
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        companyService.deleteCompany(
                companyId,
                userId
        );

        return ResponseEntity.ok(CommonResponse.success("업체가 성공적으로 삭제되었습니다."));
    }
}
