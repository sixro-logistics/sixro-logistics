package com.sixro.logistics.delivery.infrastructure.client.hub;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.HubInfo;
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
class HubClientAdapterTest {

    private static final UUID HUB_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Mock
    private HubClient hubClient;

    @InjectMocks
    private HubClientAdapter adapter;

    @Test
    @DisplayName("Hub Service의 정상 응답을 Application 허브 정보로 변환한다")
    void findHub_success_mapsHubInfo() {
        // given
        HubClientResponse responseData = new HubClientResponse(HUB_ID, "서울 허브");
        when(hubClient.getHub(HUB_ID))
                .thenReturn(CommonResponse.success("허브 조회 성공", responseData));

        // when
        Optional<HubInfo> result = adapter.findHub(HUB_ID);

        // then
        assertThat(result).contains(new HubInfo(HUB_ID, "서울 허브"));
    }

    @Test
    @DisplayName("Hub Service가 404를 반환하면 빈 Optional을 반환한다")
    void findHub_notFound_returnsEmpty() {
        // given
        when(hubClient.getHub(HUB_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        // when
        Optional<HubInfo> result = adapter.findHub(HUB_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Hub Service가 유효하지 않은 응답을 반환하면 C999 예외가 발생한다")
    void findHub_invalidResponse_throwsInternalServerError() {
        // given
        when(hubClient.getHub(HUB_ID)).thenReturn(null);

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> adapter.findHub(HUB_ID)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }
}
