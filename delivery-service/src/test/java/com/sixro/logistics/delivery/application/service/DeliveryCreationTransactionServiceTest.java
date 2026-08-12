package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.delivery.application.event.DeliveryCreatedEvent;
import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryCreationTransactionServiceTest {

    private static final String TRACE_ID = "delivery-creation-trace-id";
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DELIVERY_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MIDDLE_HUB_ID_1 = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID MIDDLE_HUB_ID_2 = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID DEST_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID SUPPLIER_COMPANY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID RECIPIENT_COMPANY_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final LocalDateTime DELIVERY_DEADLINE = LocalDateTime.of(2026, 8, 14, 18, 0);

    @Mock
    private DeliveryRepositoryPort deliveryRepositoryPort;

    @Mock
    private DeliveryRouteRepositoryPort deliveryRouteRepositoryPort;

    @Mock
    private DeliveryManagerRepositoryPort deliveryManagerRepositoryPort;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private DeliveryCreationTransactionService transactionService;

    @Test
    @DisplayName("배송과 배송경로를 저장하고 다음 순번 담당자를 자동 배정한다")
    void create_savesDeliveryAndRoutesWithNextManagers() {
        // given
        DeliveryCreationData data = createData(createThreeRoutes());
        DeliveryManager hubManager1 = createHubManager(1);
        DeliveryManager hubManager3 = createHubManager(3);
        DeliveryManager hubManager6 = createHubManager(6);
        DeliveryManager companyManager1 = createCompanyManager(1);
        DeliveryManager companyManager4 = createCompanyManager(4);
        DeliveryManager companyManager7 = createCompanyManager(7);

        when(deliveryRepositoryPort.existsByOrderIdIncludingDeleted(ORDER_ID)).thenReturn(false);
        when(deliveryManagerRepositoryPort.findAvailableHubManagersForUpdate())
                .thenReturn(List.of(hubManager1, hubManager3, hubManager6));
        when(deliveryManagerRepositoryPort.findAvailableCompanyManagersForUpdate(DEST_HUB_ID))
                .thenReturn(List.of(companyManager1, companyManager4, companyManager7));
        when(deliveryManagerRepositoryPort.findLastAssignedHubManagerSequence())
                .thenReturn(Optional.of(1));
        when(deliveryManagerRepositoryPort.findLastAssignedCompanyManagerSequence(DEST_HUB_ID))
                .thenReturn(Optional.of(4));
        stubDeliverySave();

        // when
        boolean result = transactionService.create(data);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepositoryPort).save(deliveryCaptor.capture());
        Delivery savedDelivery = deliveryCaptor.getValue();
        assertThat(savedDelivery.getDeliveryId()).isEqualTo(DELIVERY_ID);
        assertThat(savedDelivery.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(savedDelivery.getSupplierCompanyIds()).containsExactly(SUPPLIER_COMPANY_ID);
        assertThat(savedDelivery.getDeliveryManager()).isSameAs(companyManager7);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryRoute>> routesCaptor = ArgumentCaptor.forClass(List.class);
        verify(deliveryRouteRepositoryPort).saveAll(routesCaptor.capture());
        List<DeliveryRoute> savedRoutes = routesCaptor.getValue();
        assertThat(savedRoutes).hasSize(3);
        assertThat(savedRoutes).extracting(DeliveryRoute::getRouteSequence)
                .containsExactly(1, 2, 3);
        assertThat(savedRoutes).extracting(DeliveryRoute::getDeliveryManager)
                .containsExactly(hubManager3, hubManager6, hubManager1);
        assertThat(savedRoutes).allSatisfy(route ->
                assertThat(route.getDelivery()).isSameAs(savedDelivery));

        ArgumentCaptor<DeliveryCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreatedEvent.class);
        verify(outboxService).save(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq(TRACE_ID));
        DeliveryCreatedEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.data().orderId()).isEqualTo(ORDER_ID);
        assertThat(event.data().deliveryId()).isEqualTo(DELIVERY_ID);
        assertThat(event.data().products())
                .containsExactly(new DeliveryCreatedEvent.Product(PRODUCT_ID, 4));
        assertThat(event.data().routes()).extracting(DeliveryCreatedEvent.Route::sequence)
                .containsExactly(1, 2, 3);
        assertThat(event.data().totalHubRouteExpectedDurationS()).isEqualTo(18_000L);
        assertThat(event.data().deliveryManagerWorkingHours().startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(event.data().deliveryManagerWorkingHours().endTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(event.data().deliveryManagers())
                .extracting(DeliveryCreatedEvent.DeliveryManagerInfo::deliverySequence)
                .containsExactly(1, 3, 6, 7);
    }

    @Test
    @DisplayName("이미 처리한 주문이면 배송 생성과 외부 저장을 생략한다")
    void create_duplicateOrder_skipsCreation() {
        // given
        DeliveryCreationData data = createData(createThreeRoutes());
        when(deliveryRepositoryPort.existsByOrderIdIncludingDeleted(ORDER_ID)).thenReturn(true);

        // when
        boolean result = transactionService.create(data);

        // then
        assertThat(result).isFalse();
        verify(deliveryRepositoryPort, never()).save(any());
        verifyNoInteractions(deliveryRouteRepositoryPort, deliveryManagerRepositoryPort, outboxService);
    }

    @Test
    @DisplayName("최근 배정 순번이 3이면 다음 순번인 업체 배송 담당자 4를 배정한다")
    void create_lastCompanySequenceThree_assignsSequenceFour() {
        // given
        DeliveryCreationData data = createData(List.of());
        DeliveryManager companyManager1 = createCompanyManager(1);
        DeliveryManager companyManager2 = createCompanyManager(2);
        DeliveryManager companyManager3 = createCompanyManager(3);
        DeliveryManager companyManager4 = createCompanyManager(4);

        when(deliveryRepositoryPort.existsByOrderIdIncludingDeleted(ORDER_ID)).thenReturn(false);
        when(deliveryManagerRepositoryPort.findAvailableCompanyManagersForUpdate(DEST_HUB_ID))
                .thenReturn(List.of(companyManager1, companyManager2, companyManager3, companyManager4));
        when(deliveryManagerRepositoryPort.findLastAssignedCompanyManagerSequence(DEST_HUB_ID))
                .thenReturn(Optional.of(3));
        stubDeliverySave();

        // when
        boolean result = transactionService.create(data);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepositoryPort).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getDeliveryManager()).isSameAs(companyManager4);
        assertThat(deliveryCaptor.getValue().getDeliveryManager().getDeliverySequence()).isEqualTo(4);
    }

    @Test
    @DisplayName("마지막 담당자 순번 이후에는 첫 번째 활성 담당자부터 다시 배정한다")
    void create_lastSequence_wrapsToFirstManager() {
        // given
        DeliveryCreationData data = createData(createThreeRoutes());
        DeliveryManager hubManager1 = createHubManager(1);
        DeliveryManager hubManager5 = createHubManager(5);
        DeliveryManager hubManager10 = createHubManager(10);
        DeliveryManager companyManager1 = createCompanyManager(1);
        DeliveryManager companyManager10 = createCompanyManager(10);

        when(deliveryRepositoryPort.existsByOrderIdIncludingDeleted(ORDER_ID)).thenReturn(false);
        when(deliveryManagerRepositoryPort.findAvailableHubManagersForUpdate())
                .thenReturn(List.of(hubManager1, hubManager5, hubManager10));
        when(deliveryManagerRepositoryPort.findAvailableCompanyManagersForUpdate(DEST_HUB_ID))
                .thenReturn(List.of(companyManager1, companyManager10));
        when(deliveryManagerRepositoryPort.findLastAssignedHubManagerSequence())
                .thenReturn(Optional.of(10));
        when(deliveryManagerRepositoryPort.findLastAssignedCompanyManagerSequence(DEST_HUB_ID))
                .thenReturn(Optional.of(10));
        stubDeliverySave();

        // when
        transactionService.create(data);

        // then
        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepositoryPort).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getDeliveryManager()).isSameAs(companyManager1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryRoute>> routesCaptor = ArgumentCaptor.forClass(List.class);
        verify(deliveryRouteRepositoryPort).saveAll(routesCaptor.capture());
        assertThat(routesCaptor.getValue()).extracting(DeliveryRoute::getDeliveryManager)
                .containsExactly(hubManager1, hubManager5, hubManager10);
    }

    @Test
    @DisplayName("근무 가능한 담당자가 없으면 담당자를 비워둔 채 배송과 경로를 생성한다")
    void create_noAvailableManagers_createsWithoutAssignment() {
        // given
        DeliveryCreationData data = createData(createThreeRoutes());
        when(deliveryRepositoryPort.existsByOrderIdIncludingDeleted(ORDER_ID)).thenReturn(false);
        when(deliveryManagerRepositoryPort.findAvailableHubManagersForUpdate()).thenReturn(List.of());
        when(deliveryManagerRepositoryPort.findAvailableCompanyManagersForUpdate(DEST_HUB_ID))
                .thenReturn(List.of());
        stubDeliverySave();

        // when
        boolean result = transactionService.create(data);

        // then
        assertThat(result).isTrue();

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepositoryPort).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getDeliveryManager()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryRoute>> routesCaptor = ArgumentCaptor.forClass(List.class);
        verify(deliveryRouteRepositoryPort).saveAll(routesCaptor.capture());
        assertThat(routesCaptor.getValue()).allSatisfy(route ->
                assertThat(route.getDeliveryManager()).isNull());
        verify(deliveryManagerRepositoryPort, never()).findLastAssignedHubManagerSequence();
        verify(deliveryManagerRepositoryPort, never())
                .findLastAssignedCompanyManagerSequence(any());
        verify(outboxService).save(any(DeliveryCreatedEvent.class), org.mockito.ArgumentMatchers.eq(TRACE_ID));
    }

    @Test
    @DisplayName("출발 허브와 목적지 허브가 같아 경로가 없으면 허브 담당자 조회와 경로 저장을 생략한다")
    void create_withoutHubRoutes_skipsHubManagerLookupAndRouteSave() {
        // given
        DeliveryCreationData data = createData(List.of());
        DeliveryManager companyManager = createCompanyManager(2);
        when(deliveryRepositoryPort.existsByOrderIdIncludingDeleted(ORDER_ID)).thenReturn(false);
        when(deliveryManagerRepositoryPort.findAvailableCompanyManagersForUpdate(DEST_HUB_ID))
                .thenReturn(List.of(companyManager));
        when(deliveryManagerRepositoryPort.findLastAssignedCompanyManagerSequence(DEST_HUB_ID))
                .thenReturn(Optional.empty());
        stubDeliverySave();

        // when
        boolean result = transactionService.create(data);

        // then
        assertThat(result).isTrue();
        verify(deliveryManagerRepositoryPort, never()).findAvailableHubManagersForUpdate();
        verify(deliveryManagerRepositoryPort, never()).findLastAssignedHubManagerSequence();
        verify(deliveryRouteRepositoryPort, never()).saveAll(any());

        ArgumentCaptor<DeliveryCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCreatedEvent.class);
        verify(outboxService).save(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq(TRACE_ID));
        assertThat(eventCaptor.getValue().data().routes()).isEmpty();
        assertThat(eventCaptor.getValue().data().totalHubRouteExpectedDurationS()).isZero();
    }

    // JPA가 생성하는 배송 ID를 단위 테스트에서 재현합니다.
    private void stubDeliverySave() {
        when(deliveryRepositoryPort.save(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            ReflectionTestUtils.setField(delivery, "deliveryId", DELIVERY_ID);
            return delivery;
        });
    }

    // 배송 생성 트랜잭션에 전달되는 정상 데이터를 구성합니다.
    private DeliveryCreationData createData(List<DeliveryCreationData.RouteData> routes) {
        return new DeliveryCreationData(
                TRACE_ID,
                ORDER_ID,
                List.of(new DeliveryCreationData.ProductData(PRODUCT_ID, 4)),
                Set.of(SUPPLIER_COMPANY_ID),
                RECIPIENT_COMPANY_ID,
                ORIGIN_HUB_ID,
                DEST_HUB_ID,
                "서울특별시 중구 세종대로 1",
                DELIVERY_DEADLINE,
                "도착 전 연락",
                "홍길동",
                "U-RECIPIENT",
                routes
        );
    }

    // 세 개의 연속된 허브 이동 경로를 구성합니다.
    private List<DeliveryCreationData.RouteData> createThreeRoutes() {
        return List.of(
                new DeliveryCreationData.RouteData(1, ORIGIN_HUB_ID, MIDDLE_HUB_ID_1, 100_000L, 5_000L),
                new DeliveryCreationData.RouteData(2, MIDDLE_HUB_ID_1, MIDDLE_HUB_ID_2, 120_000L, 6_000L),
                new DeliveryCreationData.RouteData(3, MIDDLE_HUB_ID_2, DEST_HUB_ID, 140_000L, 7_000L)
        );
    }

    private DeliveryManager createHubManager(int sequence) {
        return DeliveryManager.create(UUID.randomUUID(), null, ManagerType.HUB_DELIVERY, sequence);
    }

    private DeliveryManager createCompanyManager(int sequence) {
        return DeliveryManager.create(UUID.randomUUID(), DEST_HUB_ID, ManagerType.COMPANY_DELIVERY, sequence);
    }
}
