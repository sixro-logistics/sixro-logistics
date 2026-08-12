package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.infrastructure.client.AffiliationClientReader;
import com.sixro.logistics.user.infrastructure.client.response.InternalCompanyResponse;
import com.sixro.logistics.user.infrastructure.client.response.InternalHubResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliationValidationService {

    /*
     * FeignClient를 직접 호출하지 않고 Circuit Breaker가 적용된
     * AffiliationClientReader를 통해 Hub·Company Service를 호출합니다.
     */
    private final AffiliationClientReader affiliationClientReader;

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

    /**
     * Hub Service를 호출하여 허브 존재 여부를 검증합니다.
     */
    private void validateHub(UUID hubId) {
        try {
            CommonResponse<InternalHubResponse> response =
                    affiliationClientReader.getHub(hubId);

            validateHubResponse(hubId, response);

        } catch (CallNotPermittedException exception) {
            /*
             * Circuit Breaker가 OPEN 상태이면 실제 Hub Service를
             * 호출하지 않고 즉시 서비스 이용 불가 오류로 변환합니다.
             */
            handleCircuitOpen(
                    "Hub",
                    hubId,
                    exception
            );

        } catch (FeignException exception) {
            handleFeignException(
                    "Hub",
                    hubId,
                    exception
            );
        }
    }

    /**
     * Company Service를 호출하여 업체 존재 여부를 검증합니다.
     */
    private void validateCompany(UUID companyId) {
        try {
            CommonResponse<InternalCompanyResponse> response =
                    affiliationClientReader.getCompany(companyId);

            validateCompanyResponse(
                    companyId,
                    response
            );

        } catch (CallNotPermittedException exception) {
            /*
             * Circuit Breaker가 OPEN 상태이면 실제 Company Service를
             * 호출하지 않고 즉시 서비스 이용 불가 오류로 변환합니다.
             */
            handleCircuitOpen(
                    "Company",
                    companyId,
                    exception
            );

        } catch (FeignException exception) {
            handleFeignException(
                    "Company",
                    companyId,
                    exception
            );
        }
    }

    /**
     * Hub Service의 정상 응답 구조와 요청한 허브 ID를 검증합니다.
     */
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
                    "Hub Service 응답 ID가 일치하지 않습니다. "
                            + "requestedHubId={}, responseHubId={}",
                    requestedHubId,
                    response.data().hubId()
            );

            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }
    }

    /**
     * Company Service의 정상 응답 구조와 요청한 업체 ID를 검증합니다.
     */
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
                    "Company Service 응답 ID가 일치하지 않습니다. "
                            + "requestedCompanyId={}, responseCompanyId={}",
                    requestedCompanyId,
                    response.data().companyId()
            );

            throw new BaseException(
                    UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
            );
        }
    }

    /**
     * Circuit Breaker OPEN 상태를 소속 서비스 이용 불가 오류로 변환합니다.
     */
    private void handleCircuitOpen(
            String serviceName,
            UUID affiliationId,
            CallNotPermittedException exception
    ) {
        log.warn(
                "{} Service Circuit Breaker가 OPEN 상태입니다. "
                        + "affiliationId={}",
                serviceName,
                affiliationId,
                exception
        );

        throw new BaseException(
                UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
        );
    }

    /**
     * FeignClient 호출 오류를 비즈니스 오류로 변환합니다.
     *
     * <p>404와 410은 실제 소속 데이터가 존재하지 않는 경우이고,
     * 나머지 통신 오류와 5xx 응답은 소속 서비스 장애로 처리합니다.</p>
     */
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