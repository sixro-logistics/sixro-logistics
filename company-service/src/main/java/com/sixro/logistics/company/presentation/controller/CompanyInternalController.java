package com.sixro.logistics.company.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.company.application.service.CompanyInternalService;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalCheckResponseDto;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalHubInfoResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Company", description = "업체 관련 내부 API")
@RestController
@RequestMapping("/api/v1/internal/companies")
@RequiredArgsConstructor
public class CompanyInternalController {

    private final CompanyInternalService CompanyInternalService;

    @Operation(
            summary = "업체 존재 유무 조회",
            description = "업체 존재 유무를 조회합니다."
    )
    @GetMapping("/{companyId}")
    public CommonResponse<CompanyInternalCheckResponseDto> getCheckCompany(
            @PathVariable("companyId") UUID companyId
    ) {
        CompanyInternalCheckResponseDto response = CompanyInternalService.getCheckCompany(companyId);
        return CommonResponse.success("유효한 업체입니다.", response);
    }

    @Operation(
            summary = "목적지 허브 조회",
            description = "목적지 허브를 조회하여 반환합니다."
    )
    @GetMapping("/hub-info")
    public ResponseEntity<CommonResponse<CompanyInternalHubInfoResponseDto>> getHubInfo(
            @RequestParam("companyId") UUID companyId
    ) {
        CompanyInternalHubInfoResponseDto response = CompanyInternalService.getCompanyHubInfo(companyId);
        return ResponseEntity.ok(CommonResponse.success("목적지 허브를 반환에 성공하였습니다.", response));
    }
}
