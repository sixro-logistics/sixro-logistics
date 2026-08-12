package com.sixro.logistics.company.presentation.controller;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.company.application.service.CompanyService;
import com.sixro.logistics.company.presentation.dto.request.CompanyCreateRequestDto;
import com.sixro.logistics.company.presentation.dto.request.CompanyUpdateRequestDto;
import com.sixro.logistics.company.presentation.dto.response.CompanyResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Company", description = "업체 정보 조회, 상세 조회, 등록, 수정, 삭제 API")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "업체 목록 조회",
            description = "모든 업체 목록 정보를 조회합니다."
    )
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

    @Operation(
            summary = "업체 상세 조회",
            description = "(단건) 업체 상세 조회 정보를 조회합니다."
    )
    @GetMapping("/{companyId}")
    public ResponseEntity<CommonResponse<CompanyResponseDto>> getCompany(
            @PathVariable UUID companyId
    ) {
        return ResponseEntity.ok(CommonResponse.success("업체 목록 조회에 성공했습니다.", companyService.getCompany(companyId)));
    }

    @Operation(
            summary = "업체 등록",
            description = "신규 업체를 등록합니다."
    )
    @PostMapping
    public ResponseEntity<CompanyResponseDto> createCompany(
            @Valid @RequestBody CompanyCreateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(
                        value = "X-Affiliation-Id",
                        required = false
                        ) UUID affiliationId
    ) {
        if ("MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            // MASTER_ADMIN은 소속 정보 없이 모든 허브에 업체를 생성할 수 있습니다.
        } else if ("HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 자신의 담당 허브에만 업체를 생성할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(request.getHubId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        CompanyResponseDto response = companyService.createCompany(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "업체 수정",
            description = "기존 업체를 수정합니다."
    )
    @PatchMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDto> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyUpdateRequestDto request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(
                    value = "X-Affiliation-Id",
                    required = false
            ) UUID affiliationId
    ) {

        if ("MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            // MASTER_ADMIN은 소속 정보 없이 모든 허브에 업체를 수정할 수 있습니다.
        } else if ("HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 자신의 담당 허브에만 업체를 수정할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(request.getHubId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else if("COMPANY_MANAGER".equalsIgnoreCase(userRole)) {
            // COMPANY_MANAGER는 본인 업체에 대한 업체만 수정할 수 있습니다.
            if (affiliationId == null || !affiliationId.equals(companyId)) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        CompanyResponseDto response =
                companyService.updateCompany(
                        companyId,
                        request,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "업체 삭제",
            description = "기존 업체를 삭제합니다."
    )
    @DeleteMapping("/{companyId}")
    public ResponseEntity<CommonResponse<Void>> deleteCompany(
            @PathVariable UUID companyId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(
                        value = "X-Affiliation-Id",
                        required = false
                ) UUID affiliationId
    ) {
        if ("MASTER_ADMIN".equalsIgnoreCase(userRole)) {
            // MASTER_ADMIN은 소속 정보 없이 모든 허브에 업체를 생성할 수 있습니다.
        } else if ("HUB_ADMIN".equalsIgnoreCase(userRole)) {
            // HUB_ADMIN은 자신의 담당 허브에만 업체를 생성할 수 있습니다.
            CompanyResponseDto resCompanyDto = companyService.getCompany(companyId);

            if (affiliationId == null || !affiliationId.equals(resCompanyDto.getHubId())) {
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        } else {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        companyService.deleteCompany(
                companyId,
                userId
        );

        return ResponseEntity.ok(CommonResponse.success("업체가 성공적으로 삭제되었습니다."));
    }
}
