package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.delivery.application.result.DeliveryManagerIdsResult;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.exception.DeliveryErrorCode;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryInternalServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DELIVERY_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID DEST_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private DeliveryRepositoryPort deliveryRepositoryPort;

    @Mock
    private DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    @InjectMocks
    private DeliveryInternalService deliveryInternalService;

    @Test
    @DisplayName("주문 배송의 허브 및 업체 배송 담당자 ID를 중복 없이 조회한다")
    void getDeliveryManagerIds_returnsDistinctAssignedManagerIds() {
        // given
        DeliveryManager hubManager = createHubManager(1);
        DeliveryManager companyManager = createCompanyManager(1);
        Delivery delivery = createDelivery();
        delivery.assignDeliveryManager(companyManager);

        DeliveryRoute route1 = createRoute(delivery, 1, ORIGIN_HUB_ID, DEST_HUB_ID);
        route1.assignDeliveryManager(hubManager);
        DeliveryRoute route2 = createRoute(delivery, 2, DEST_HUB_ID, ORIGIN_HUB_ID);
        route2.assignDeliveryManager(hubManager);
        DeliveryRoute unassignedRoute = createRoute(delivery, 3, ORIGIN_HUB_ID, DEST_HUB_ID);

        when(deliveryRepositoryPort.findByOrderId(ORDER_ID)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepositoryPort.findAllByDeliveryId(DELIVERY_ID))
                .thenReturn(List.of(route1, route2, unassignedRoute));

        // when
        DeliveryManagerIdsResult result = deliveryInternalService.getDeliveryManagerIds(ORDER_ID);

        // then
        assertThat(result.getDeliveryManagerIds())
                .containsExactlyInAnyOrder(
                        hubManager.getDeliveryManagerId(),
                        companyManager.getDeliveryManagerId()
                );
        assertThat(result.getDeliveryManagerIds()).doesNotHaveDuplicates();
        verify(deliveryRepositoryPort).findByOrderId(ORDER_ID);
        verify(deliveryRouteRepositoryPort).findAllByDeliveryId(DELIVERY_ID);
    }

    @Test
    @DisplayName("배송과 경로에 배정된 담당자가 없으면 빈 ID 목록을 반환한다")
    void getDeliveryManagerIds_withoutAssignedManagers_returnsEmptyList() {
        // given
        Delivery delivery = createDelivery();
        DeliveryRoute unassignedRoute = createRoute(delivery, 1, ORIGIN_HUB_ID, DEST_HUB_ID);
        when(deliveryRepositoryPort.findByOrderId(ORDER_ID)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepositoryPort.findAllByDeliveryId(DELIVERY_ID))
                .thenReturn(List.of(unassignedRoute));

        // when
        DeliveryManagerIdsResult result = deliveryInternalService.getDeliveryManagerIds(ORDER_ID);

        // then
        assertThat(result.getDeliveryManagerIds()).isEmpty();
    }

    @Test
    @DisplayName("주문에 해당하는 배송이 없으면 D012 예외가 발생한다")
    void getDeliveryManagerIds_deliveryNotFound_throwsDeliveryNotFound() {
        // given
        when(deliveryRepositoryPort.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryInternalService.getDeliveryManagerIds(ORDER_ID)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(DeliveryErrorCode.DELIVERY_NOT_FOUND);
        verify(deliveryRouteRepositoryPort, never()).findAllByDeliveryId(org.mockito.ArgumentMatchers.any());
    }

    private Delivery createDelivery() {
        Delivery delivery = Delivery.create(
                ORDER_ID,
                Set.of(UUID.randomUUID()),
                UUID.randomUUID(),
                ORIGIN_HUB_ID,
                DEST_HUB_ID,
                "서울특별시 중구 세종대로 1",
                LocalDateTime.of(2026, 8, 14, 18, 0),
                null,
                "홍길동",
                "U-RECIPIENT"
        );
        ReflectionTestUtils.setField(delivery, "deliveryId", DELIVERY_ID);
        return delivery;
    }

    private DeliveryRoute createRoute(
            Delivery delivery,
            int sequence,
            UUID originHubId,
            UUID destHubId
    ) {
        return DeliveryRoute.create(delivery, sequence, originHubId, destHubId, 100_000L, 5_400L);
    }

    private DeliveryManager createHubManager(int sequence) {
        return DeliveryManager.create(UUID.randomUUID(), null, ManagerType.HUB_DELIVERY, sequence);
    }

    private DeliveryManager createCompanyManager(int sequence) {
        return DeliveryManager.create(UUID.randomUUID(), DEST_HUB_ID, ManagerType.COMPANY_DELIVERY, sequence);
    }
}
