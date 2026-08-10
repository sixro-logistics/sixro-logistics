package com.sixro.logistics.company.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.company.application.service.CompanyInternalService;
import com.sixro.logistics.company.presentation.dto.response.internal.CompanyInternalCheckResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/internal")
@RequiredArgsConstructor
public class CompanyInternalController {

    private final CompanyInternalService CompanyInternalService;

    @GetMapping("/check/{companyId}")
    public CommonResponse<CompanyInternalCheckResponseDto> getCheckCompany(
            @PathVariable("companyId") UUID companyId
    ) {
        CompanyInternalCheckResponseDto response = CompanyInternalService.getCheckCompany(companyId);
        return CommonResponse.success("유효한 업체입니다.", response);
    }
}
