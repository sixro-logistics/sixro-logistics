package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.delivery.application.command.UpdateDeliveryRouteStatusCommand;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteServiceTest {

    private static final UUID DELIVERY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DELIVERY_ROUTE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEST_HUB_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");

    @Mock
    private DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    @Mock
    private DeliveryRepositoryPort deliveryRepositoryPort;

    @Mock
    private DeliveryManagerRepositoryPort deliveryManagerRepositoryPort;

    @InjectMocks
    private DeliveryRouteService deliveryRouteService;

    @Test
    @DisplayName("배송경로 상태 변경 시 배송을 먼저 잠그고 배송경로를 잠근다")
    void updateDeliveryRouteStatus_locksDeliveryBeforeDeliveryRoute() {
        // given
        Delivery delivery = createDelivery();
        DeliveryRoute deliveryRoute = createDeliveryRoute(delivery);
        UpdateDeliveryRouteStatusCommand command = new UpdateDeliveryRouteStatusCommand(
                DELIVERY_ROUTE_ID, RouteStatus.FAILED, UUID.randomUUID(), "MASTER_ADMIN", null);

        when(deliveryRouteRepositoryPort.findDeliveryIdById(DELIVERY_ROUTE_ID))
                .thenReturn(Optional.of(DELIVERY_ID));
        when(deliveryRepositoryPort.findByIdForUpdate(DELIVERY_ID))
                .thenReturn(Optional.of(delivery));
        when(deliveryRouteRepositoryPort.findByIdForUpdate(DELIVERY_ROUTE_ID))
                .thenReturn(Optional.of(deliveryRoute));

        // when
        deliveryRouteService.updateDeliveryRouteStatus(command);

        // then
        InOrder lockOrder = inOrder(deliveryRouteRepositoryPort, deliveryRepositoryPort);
        lockOrder.verify(deliveryRouteRepositoryPort).findDeliveryIdById(DELIVERY_ROUTE_ID);
        lockOrder.verify(deliveryRepositoryPort).findByIdForUpdate(DELIVERY_ID);
        lockOrder.verify(deliveryRouteRepositoryPort).findByIdForUpdate(DELIVERY_ROUTE_ID);

        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(deliveryRoute.getRouteStatus()).isEqualTo(RouteStatus.FAILED);
        verify(deliveryRouteRepositoryPort).flush();
    }

    private Delivery createDelivery() {
        Delivery delivery = Delivery.create(
                UUID.randomUUID(), Set.of(UUID.randomUUID()), UUID.randomUUID(),
                ORIGIN_HUB_ID, DEST_HUB_ID, "배송 주소", null, null,
                "수령인", "U-RECIPIENT");
        ReflectionTestUtils.setField(delivery, "deliveryId", DELIVERY_ID);
        return delivery;
    }

    private DeliveryRoute createDeliveryRoute(Delivery delivery) {
        DeliveryRoute deliveryRoute = DeliveryRoute.create(
                delivery, 1, ORIGIN_HUB_ID, DEST_HUB_ID, 1000L, 600L);
        ReflectionTestUtils.setField(deliveryRoute, "deliveryRouteId", DELIVERY_ROUTE_ID);
        return deliveryRoute;
    }
}
