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
import feign.Request;
import feign.Response;
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
import static org.mockito.Mockito.when;

/**
 * 사용자 소속 검증 서비스의 단위 테스트입니다.
 *
 * <p>Hub/Company Service Client를 Mock으로 대체하여
 * 소속 유형별 호출, 정상 응답, 잘못된 응답 및
 * Feign 예외 변환 처리를 검증합니다.</p>
 *
 * <p>실제 외부 서비스나 네트워크는 호출하지 않습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AffiliationValidationServiceTest {

    @Mock
    private HubServiceClient hubServiceClient;

    @Mock
    private CompanyServiceClient companyServiceClient;

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

        when(hubServiceClient.getHub(hubId))
                .thenReturn(CommonResponse.success("허브 조회 성공", data));

        assertThatCode(
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        ).doesNotThrowAnyException();

        verify(hubServiceClient).getHub(hubId);
        verifyNoInteractions(companyServiceClient);
    }

    @Test
    @DisplayName("존재하는 업체 소속이면 검증에 성공한다")
    void validateCompany_success() {
        UUID companyId = UUID.randomUUID();
        InternalCompanyResponse data = new InternalCompanyResponse(
                companyId,
                "육로물류"
        );

        when(companyServiceClient.getCompany(companyId))
                .thenReturn(CommonResponse.success("업체 조회 성공", data));

        assertThatCode(
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        ).doesNotThrowAnyException();

        verify(companyServiceClient).getCompany(companyId);
        verifyNoInteractions(hubServiceClient);
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
                .isEqualTo(UserErrorCode.INVALID_AFFILIATION);
        verifyNoInteractions(hubServiceClient, companyServiceClient);
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
                .isEqualTo(UserErrorCode.INVALID_AFFILIATION);
        verifyNoInteractions(hubServiceClient, companyServiceClient);
    }

    @Test
    @DisplayName("소속 서비스가 404를 반환하면 AFFILIATION_NOT_FOUND로 변환한다")
    void validateHub_notFound() {
        UUID hubId = UUID.randomUUID();

        when(hubServiceClient.getHub(hubId))
                .thenThrow(feignException(
                        404,
                        "http://hub-service/api/v1/internal/hubs/" + hubId
                ));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성화된 업체가 410을 반환하면 AFFILIATION_NOT_FOUND로 변환한다")
    void validateCompany_gone() {
        UUID companyId = UUID.randomUUID();

        when(companyServiceClient.getCompany(companyId))
                .thenThrow(feignException(
                        410,
                        "http://company-service/api/v1/internal/companies/"
                                + companyId
                ));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_NOT_FOUND);
    }

    @Test
    @DisplayName("소속 서비스의 5xx 오류는 AFFILIATION_SERVICE_UNAVAILABLE로 변환한다")
    void validateHub_serverError() {
        UUID hubId = UUID.randomUUID();

        when(hubServiceClient.getHub(hubId))
                .thenThrow(feignException(
                        503,
                        "http://hub-service/api/v1/internal/hubs/" + hubId
                ));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("성공 응답에 data가 없으면 소속 서비스 오류로 처리한다")
    void validateCompany_nullData() {
        UUID companyId = UUID.randomUUID();

        when(companyServiceClient.getCompany(companyId))
                .thenReturn(CommonResponse.success("업체 조회 성공", null));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        companyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("응답의 success가 false이면 소속 서비스 오류로 처리한다")
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

        when(hubServiceClient.getHub(hubId))
                .thenReturn(response);

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        hubId,
                        AffiliationType.HUB
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("요청 ID와 응답 ID가 다르면 소속 서비스 오류로 처리한다")
    void validateCompany_mismatchedResponseId() {
        UUID requestedCompanyId = UUID.randomUUID();
        InternalCompanyResponse data = new InternalCompanyResponse(
                UUID.randomUUID(),
                "다른 업체"
        );

        when(companyServiceClient.getCompany(requestedCompanyId))
                .thenReturn(CommonResponse.success("업체 조회 성공", data));

        BaseException exception = assertThrows(
                BaseException.class,
                () -> affiliationValidationService.validate(
                        requestedCompanyId,
                        AffiliationType.COMPANY
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_SERVICE_UNAVAILABLE);
    }

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
}
