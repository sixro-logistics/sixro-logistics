package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.infrastructure.client.AffiliationClientReader;
import com.sixro.logistics.user.infrastructure.client.response.InternalCompanyResponse;
import com.sixro.logistics.user.infrastructure.client.response.InternalHubResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 사용자 소속 검증 서비스의 단위 테스트입니다.
 *
 * <p>Circuit Breaker가 적용된 AffiliationClientReader를 Mock으로 대체하여
 * 소속 유형별 호출, 정상 응답, 잘못된 응답, Feign 예외 변환 및
 * Circuit Breaker OPEN 상태 처리를 검증합니다.</p>
 *
 * <p>실제 Hub·Company Service나 네트워크는 호출하지 않습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AffiliationValidationServiceTest {

    @Mock
    private AffiliationClientReader affiliationClientReader;

    @InjectMocks
    private AffiliationValidationService affiliationValidationService;

    @Test
    @DisplayName("존재하는 허브 소속이면 검증에 성공한다")
    void validateHub_success() {
        UUID hubId = UUID.randomUUID();
        InternalHubResponse data = new InternalHubResponse(
                hubId,
                "서울 중앙 허브"
        );

        when(affiliationClientReader.getHub(hubId))
                .thenReturn(
                        CommonResponse.success(
                                "허브 조회 성공",
                                data
                        )
                );

        assertThatCode(
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        ).doesNotThrowAnyException();

        verify(affiliationClientReader).getHub(hubId);
        verifyNoMoreInteractions(affiliationClientReader);
    }

    @Test
    @DisplayName("존재하는 업체 소속이면 검증에 성공한다")
    void validateCompany_success() {
        UUID companyId = UUID.randomUUID();
        InternalCompanyResponse data = new InternalCompanyResponse(
                companyId,
                "식스로물류"
        );

        when(affiliationClientReader.getCompany(companyId))
                .thenReturn(
                        CommonResponse.success(
                                "업체 조회 성공",
                                data
                        )
                );

        assertThatCode(
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        ).doesNotThrowAnyException();

        verify(affiliationClientReader).getCompany(companyId);
        verifyNoMoreInteractions(affiliationClientReader);
    }

    @Test
    @DisplayName("소속 ID가 없으면 INVALID_AFFILIATION 예외가 발생한다")
    void validate_nullAffiliationId() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        null,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.INVALID_AFFILIATION
                );

        verifyNoInteractions(affiliationClientReader);
    }

    @Test
    @DisplayName("소속 유형이 없으면 INVALID_AFFILIATION 예외가 발생한다")
    void validate_nullAffiliationType() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        UUID.randomUUID(),
                        null
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.INVALID_AFFILIATION
                );

        verifyNoInteractions(affiliationClientReader);
    }

    @Test
    @DisplayName("허브 서비스가 404를 반환하면 AFFILIATION_NOT_FOUND로 변환한다")
    void validateHub_notFound() {
        UUID hubId = UUID.randomUUID();

        when(affiliationClientReader.getHub(hubId))
                .thenThrow(
                        feignException(
                                404,
                                "http://hub-service"
                                        + "/api/v1/internal/hubs/"
                                        + hubId
                        )
                );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_NOT_FOUND
                );

        verify(affiliationClientReader).getHub(hubId);
    }

    @Test
    @DisplayName("비활성화된 업체가 410을 반환하면 AFFILIATION_NOT_FOUND로 변환한다")
    void validateCompany_gone() {
        UUID companyId = UUID.randomUUID();

        when(affiliationClientReader.getCompany(companyId))
                .thenThrow(
                        feignException(
                                410,
                                "http://company-service"
                                        + "/api/v1/internal/companies/"
                                        + companyId
                        )
                );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_NOT_FOUND
                );

        verify(affiliationClientReader)
                .getCompany(companyId);
    }

    @Test
    @DisplayName("허브 서비스의 5xx 오류는 AFFILIATION_SERVICE_UNAVAILABLE로 변환한다")
    void validateHub_serverError() {
        UUID hubId = UUID.randomUUID();

        when(affiliationClientReader.getHub(hubId))
                .thenThrow(
                        feignException(
                                503,
                                "http://hub-service"
                                        + "/api/v1/internal/hubs/"
                                        + hubId
                        )
                );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
                );

        verify(affiliationClientReader).getHub(hubId);
    }

    @Test
    @DisplayName("성공 응답에 업체 data가 없으면 소속 서비스 오류로 처리한다")
    void validateCompany_nullData() {
        UUID companyId = UUID.randomUUID();

        when(affiliationClientReader.getCompany(companyId))
                .thenReturn(
                        CommonResponse.success(
                                "업체 조회 성공",
                                null
                        )
                );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
                );

        verify(affiliationClientReader)
                .getCompany(companyId);
    }

    @Test
    @DisplayName("응답 success가 false이면 소속 서비스 오류로 처리한다")
    void validateHub_unsuccessfulResponse() {
        UUID hubId = UUID.randomUUID();

        CommonResponse<InternalHubResponse> response =
                new CommonResponse<>(
                        false,
                        503,
                        "허브 조회 실패",
                        null,
                        LocalDateTime.now()
                );

        when(affiliationClientReader.getHub(hubId))
                .thenReturn(response);

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
                );

        verify(affiliationClientReader).getHub(hubId);
    }

    @Test
    @DisplayName("요청 업체 ID와 응답 업체 ID가 다르면 소속 서비스 오류로 처리한다")
    void validateCompany_mismatchedResponseId() {
        UUID requestedCompanyId = UUID.randomUUID();

        InternalCompanyResponse data =
                new InternalCompanyResponse(
                        UUID.randomUUID(),
                        "다른 업체"
                );

        when(
                affiliationClientReader.getCompany(
                        requestedCompanyId
                )
        ).thenReturn(
                CommonResponse.success(
                        "업체 조회 성공",
                        data
                )
        );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        requestedCompanyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
                );

        verify(affiliationClientReader)
                .getCompany(requestedCompanyId);
    }

    @Test
    @DisplayName("Hub Circuit Breaker가 OPEN이면 소속 서비스 이용 불가 오류가 발생한다")
    void validateHub_circuitBreakerOpen() {
        UUID hubId = UUID.randomUUID();

        when(affiliationClientReader.getHub(hubId))
                .thenThrow(
                        callNotPermittedException(
                                "hubAffiliation"
                        )
                );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
                );

        verify(affiliationClientReader).getHub(hubId);
    }

    @Test
    @DisplayName("Company Circuit Breaker가 OPEN이면 소속 서비스 이용 불가 오류가 발생한다")
    void validateCompany_circuitBreakerOpen() {
        UUID companyId = UUID.randomUUID();

        when(affiliationClientReader.getCompany(companyId))
                .thenThrow(
                        callNotPermittedException(
                                "companyAffiliation"
                        )
                );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE
                );

        verify(affiliationClientReader)
                .getCompany(companyId);
    }

    /**
     * 테스트용 Feign HTTP 오류를 생성합니다.
     */
    private FeignException feignException(
            int status,
            String url
    ) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                url,
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );

        Response response = Response.builder()
                .status(status)
                .reason("test error")
                .request(request)
                .headers(Map.of())
                .build();

        return FeignException.errorStatus(
                "AffiliationClient#getAffiliation",
                response
        );
    }

    /**
     * OPEN 상태의 Circuit Breaker 요청 거부 예외를 생성합니다.
     */
    private CallNotPermittedException callNotPermittedException(
            String circuitBreakerName
    ) {
        CircuitBreaker circuitBreaker =
                CircuitBreaker.ofDefaults(
                        circuitBreakerName
                );

        circuitBreaker.transitionToOpenState();

        return CallNotPermittedException
                .createCallNotPermittedException(
                        circuitBreaker
                );
    }
}