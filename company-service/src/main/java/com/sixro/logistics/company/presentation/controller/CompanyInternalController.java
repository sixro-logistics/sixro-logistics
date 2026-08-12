package com.sixro.logistics.company.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.company.application.service.CompanyInternalService;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalCheckResponseDto;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalHubInfoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/companies")
@RequiredArgsConstructor
public class CompanyInternalController {

    private final CompanyInternalService CompanyInternalService;

    @GetMapping("/{companyId}")
    public CommonResponse<CompanyInternalCheckResponseDto> getCheckCompany(
            @PathVariable("companyId") UUID companyId
    ) {
        CompanyInternalCheckResponseDto response = CompanyInternalService.getCheckCompany(companyId);
        return CommonResponse.success("유효한 업체입니다.", response);
    }

    @GetMapping("/hub-info")
    public ResponseEntity<CommonResponse<CompanyInternalHubInfoResponseDto>> getHubInfo(
            @RequestParam("companyId") UUID companyId
    ) {
        CompanyInternalHubInfoResponseDto response = CompanyInternalService.getCompanyHubInfo(companyId);
        return ResponseEntity.ok(CommonResponse.success("목적지 허브를 반환에 성공하였습니다.", response));
    }
}
