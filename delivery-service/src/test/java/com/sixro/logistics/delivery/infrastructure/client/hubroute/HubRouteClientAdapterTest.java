package com.sixro.logistics.delivery.infrastructure.client.hubroute;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.HubRoutePathInfo;
import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubRouteClientAdapterTest {

    private static final UUID ORIGIN_HUB_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID MIDDLE_HUB_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID DEST_HUB_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROUTE_ID_1 = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ROUTE_ID_2 = UUID.fromString("30000000-0000-0000-0000-000000000002");

    @Mock
    private HubRouteClient hubRouteClient;

    @InjectMocks
    private HubRouteClientAdapter adapter;

    @Test
    @DisplayName("Hub Route Service의 정상 응답과 요청을 Application 경로 정보로 변환한다")
    void findPath_success_mapsRequestAndRoutePath() {
        // given
        List<HubRouteProductInfo> products = List.of(new HubRouteProductInfo(PRODUCT_ID, 4));
        HubRouteClientResponse responseData = createResponse(ORIGIN_HUB_ID, DEST_HUB_ID);
        when(hubRouteClient.getPath(org.mockito.ArgumentMatchers.eq("OPTIMAL"),
                org.mockito.ArgumentMatchers.any(HubRouteClientRequest.class)))
                .thenReturn(CommonResponse.success("허브 경로 조회 성공", responseData));

        // when
        Optional<HubRoutePathInfo> result = adapter.findPath(ORIGIN_HUB_ID, DEST_HUB_ID, products);

        // then
        ArgumentCaptor<HubRouteClientRequest> requestCaptor =
                ArgumentCaptor.forClass(HubRouteClientRequest.class);
        verify(hubRouteClient).getPath(org.mockito.ArgumentMatchers.eq("OPTIMAL"), requestCaptor.capture());

        HubRouteClientRequest request = requestCaptor.getValue();
        assertThat(request.originHubId()).isEqualTo(ORIGIN_HUB_ID);
        assertThat(request.destHubId()).isEqualTo(DEST_HUB_ID);
        assertThat(request.products())
                .containsExactly(new HubRouteClientRequest.Product(PRODUCT_ID, 4));

        assertThat(result).isPresent();
        HubRoutePathInfo pathInfo = result.orElseThrow();
        assertThat(pathInfo.originHubId()).isEqualTo(ORIGIN_HUB_ID);
        assertThat(pathInfo.destHubId()).isEqualTo(DEST_HUB_ID);
        assertThat(pathInfo.routes()).containsExactly(
                new HubRoutePathInfo.RouteInfo(
                        ROUTE_ID_1, 1, ORIGIN_HUB_ID, MIDDLE_HUB_ID, 120_000L, 5_400L
                ),
                new HubRoutePathInfo.RouteInfo(
                        ROUTE_ID_2, 2, MIDDLE_HUB_ID, DEST_HUB_ID, 180_000L, 7_200L
                )
        );
    }

    @Test
    @DisplayName("Hub Route Service가 404를 반환하면 빈 Optional을 반환한다")
    void findPath_notFound_returnsEmpty() {
        // given
        List<HubRouteProductInfo> products = List.of(new HubRouteProductInfo(PRODUCT_ID, 4));
        when(hubRouteClient.getPath(org.mockito.ArgumentMatchers.eq("OPTIMAL"),
                org.mockito.ArgumentMatchers.any(HubRouteClientRequest.class)))
                .thenThrow(mock(FeignException.NotFound.class));

        // when
        Optional<HubRoutePathInfo> result = adapter.findPath(ORIGIN_HUB_ID, DEST_HUB_ID, products);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Hub Route Service 응답 허브가 요청과 다르면 C999 예외가 발생한다")
    void findPath_mismatchedHubs_throwsInternalServerError() {
        // given
        List<HubRouteProductInfo> products = List.of(new HubRouteProductInfo(PRODUCT_ID, 4));
        HubRouteClientResponse responseData = createResponse(ORIGIN_HUB_ID, UUID.randomUUID());
        when(hubRouteClient.getPath(org.mockito.ArgumentMatchers.eq("OPTIMAL"),
                org.mockito.ArgumentMatchers.any(HubRouteClientRequest.class)))
                .thenReturn(CommonResponse.success("허브 경로 조회 성공", responseData));

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> adapter.findPath(ORIGIN_HUB_ID, DEST_HUB_ID, products)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }

    private HubRouteClientResponse createResponse(UUID originHubId, UUID destHubId) {
        return new HubRouteClientResponse(
                originHubId,
                destHubId,
                300_000L,
                12_600L,
                20_000L,
                List.of(
                        new HubRouteClientResponse.Route(
                                ROUTE_ID_1, 1, ORIGIN_HUB_ID, MIDDLE_HUB_ID,
                                120_000L, 5_400L, 8_000L
                        ),
                        new HubRouteClientResponse.Route(
                                ROUTE_ID_2, 2, MIDDLE_HUB_ID, DEST_HUB_ID,
                                180_000L, 7_200L, 12_000L
                        )
                )
        );
    }
}
