package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.infrastructure.client.CompanyServiceClient;
import com.sixro.logistics.user.infrastructure.client.HubServiceClient;
import com.sixro.logistics.user.infrastructure.client.response.InternalCompanyResponse;
import com.sixro.logistics.user.infrastructure.client.response.InternalHubResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliationValidationService {

    private final HubServiceClient hubServiceClient;
    private final CompanyServiceClient companyServiceClient;

    /**
     * 소속 유형에 따라 허브 또는 업체의 존재 여부를 검증합니다.
     */
    public void validate(
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        if (affiliationId == null
                || affiliationType == null) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }

        switch (affiliationType) {
            case HUB -> validateHub(affiliationId);
            case COMPANY -> validateCompany(affiliationId);
        }
    }

    private void validateHub(UUID hubId) {
        try {
            CommonResponse<InternalHubResponse> response =
                    hubServiceClient.getHub(hubId);

            validateHubResponse(hubId, response);

        } catch (FeignException exception) {
            handleFeignException(
                    "Hub",
                    hubId,
                    exception
            );
        }
    }

    private void validateCompany(UUID companyId) {
        try {
            CommonResponse<InternalCompanyResponse> response =
                    companyServiceClient.getCompany(companyId);

            validateCompanyResponse(
                    companyId,
                    response
            );

        } catch (FeignException exception) {
            handleFeignException(
                    "Company",
                    companyId,
                    exception
            );
        }
    }

    private void validateHubResponse(
            UUID requestedHubId,
            CommonResponse<InternalHubResponse> response
    ) {
        if (response == null
                || !response.success()
                || response.data() == null) {
            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }

        if (!requestedHubId.equals(
                response.data().hubId()
        )) {
            log.error(
                    "Hub Service 응답 ID가 일치하지 않습니다. requestedHubId={}, responseHubId={}",
                    requestedHubId,
                    response.data().hubId()
            );

            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }
    }

    private void validateCompanyResponse(
            UUID requestedCompanyId,
            CommonResponse<InternalCompanyResponse> response
    ) {
        if (response == null
                || !response.success()
                || response.data() == null) {
            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }

        if (!requestedCompanyId.equals(
                response.data().companyId()
        )) {
            log.error(
                    "Company Service 응답 ID가 일치하지 않습니다. requestedCompanyId={}, responseCompanyId={}",
                    requestedCompanyId,
                    response.data().companyId()
            );

            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }
    }

    private void handleFeignException(
            String serviceName,
            UUID affiliationId,
            FeignException exception
    ) {
        if (exception.status() == 404
                || exception.status() == 410) {
            throw new BaseException(
                    UserErrorCode.AFFILIATION_NOT_FOUND
            );
        }

        log.error(
                "{} Service 호출 실패. affiliationId={}, status={}",
                serviceName,
                affiliationId,
                exception.status(),
                exception
        );

        throw new BaseException(
                UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
        );
    }
}