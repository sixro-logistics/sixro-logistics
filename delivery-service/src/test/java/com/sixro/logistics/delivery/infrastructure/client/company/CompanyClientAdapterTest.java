package com.sixro.logistics.delivery.infrastructure.client.company;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.CompanyHubInfo;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyClientAdapterTest {

    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private CompanyClient companyClient;

    @InjectMocks
    private CompanyClientAdapter adapter;

    @Test
    @DisplayName("Company Service의 정상 응답을 Application 업체 허브 정보로 변환한다")
    void findHubInfo_success_mapsCompanyHubInfo() {
        // given
        CompanyClientResponse responseData = new CompanyClientResponse(COMPANY_ID, HUB_ID);
        when(companyClient.getHubInfo(COMPANY_ID))
                .thenReturn(CommonResponse.success("업체 허브 조회 성공", responseData));

        // when
        Optional<CompanyHubInfo> result = adapter.findHubInfo(COMPANY_ID);

        // then
        assertThat(result).contains(new CompanyHubInfo(COMPANY_ID, HUB_ID));
    }

    @Test
    @DisplayName("Company Service가 404를 반환하면 빈 Optional을 반환한다")
    void findHubInfo_notFound_returnsEmpty() {
        // given
        when(companyClient.getHubInfo(COMPANY_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        // when
        Optional<CompanyHubInfo> result = adapter.findHubInfo(COMPANY_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Company Service 응답 데이터가 없으면 C999 예외가 발생한다")
    void findHubInfo_missingData_throwsInternalServerError() {
        // given
        when(companyClient.getHubInfo(COMPANY_ID))
                .thenReturn(CommonResponse.success("업체 허브 조회 성공", null));

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> adapter.findHubInfo(COMPANY_ID)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }
}
