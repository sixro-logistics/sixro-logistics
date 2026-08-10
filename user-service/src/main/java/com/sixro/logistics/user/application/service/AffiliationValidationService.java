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
        if (affiliationId == null || affiliationType == null) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }

        switch (affiliationType) {
            case HUB -> validateHub(affiliationId);
            case COMPANY -> validateCompany(affiliationId);
        }
    }

    public void validateHub(UUID hubId) {
        try {
            CommonResponse<InternalHubResponse> response =
                    hubServiceClient.getHub(hubId);

            if (response == null
                    || !response.success()
                    || response.data() == null
                    || !hubId.equals(response.data().hubId())) {
                throw new BaseException(
                        UserErrorCode.AFFILIATION_NOT_FOUND
                );
            }

        } catch (FeignException exception) {
            if (exception.status() == 404
                    || exception.status() == 410) {
                throw new BaseException(
                        UserErrorCode.AFFILIATION_NOT_FOUND
                );
            }

            log.error(
                    "Hub Service 호출 실패. hubId={}, status={}",
                    hubId,
                    exception.status(),
                    exception
            );

            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }
    }

    public void validateCompany(UUID companyId) {
        try {
            CommonResponse<InternalCompanyResponse> response =
                    companyServiceClient.getCompany(companyId);

            if (response == null
                    || !response.success()
                    || response.data() == null
                    || !companyId.equals(
                    response.data().companyId()
            )) {
                throw new BaseException(
                        UserErrorCode.AFFILIATION_NOT_FOUND
                );
            }

        } catch (FeignException exception) {
            if (exception.status() == 404
                    || exception.status() == 410) {
                throw new BaseException(
                        UserErrorCode.AFFILIATION_NOT_FOUND
                );
            }

            log.error(
                    "Company Service 호출 실패. companyId={}, status={}",
                    companyId,
                    exception.status(),
                    exception
            );

            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }
    }
}