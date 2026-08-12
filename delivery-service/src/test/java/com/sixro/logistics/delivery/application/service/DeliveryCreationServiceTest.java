package com.sixro.logistics.delivery.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.application.command.CreateDeliveryCommand;
import com.sixro.logistics.delivery.application.command.CreateDeliveryItemCommand;
import com.sixro.logistics.delivery.application.model.CompanyHubInfo;
import com.sixro.logistics.delivery.application.model.DeliveryCreationData;
import com.sixro.logistics.delivery.application.model.HubRoutePathInfo;
import com.sixro.logistics.delivery.application.model.HubRouteProductInfo;
import com.sixro.logistics.delivery.application.model.UserInfo;
import com.sixro.logistics.delivery.application.port.CompanyQueryPort;
import com.sixro.logistics.delivery.application.port.HubRouteQueryPort;
import com.sixro.logistics.delivery.application.port.UserQueryPort;
import com.sixro.logistics.delivery.domain.exception.DeliveryCreationKafkaErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryCreationServiceTest {

    private static final String TRACE_ID = "delivery-creation-trace-id";
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORIGIN_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MIDDLE_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DEST_HUB_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID RECEIVER_COMPANY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID SUPPLIER_COMPANY_ID_1 = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID SUPPLIER_COMPANY_ID_2 = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID_1 = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID_2 = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID_3 = UUID.fromString("50000000-0000-0000-0000-000000000003");
    private static final LocalDateTime DELIVERY_DEADLINE = LocalDateTime.of(2026, 8, 14, 18, 0);

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private CompanyQueryPort companyQueryPort;

    @Mock
    private HubRouteQueryPort hubRouteQueryPort;

    @Mock
    private DeliveryCreationTransactionService transactionService;

    @InjectMocks
    private DeliveryCreationService deliveryCreationService;

    @Test
    @DisplayName("주문 생성 정보를 배송 생성 데이터로 조합한다")
    void createDelivery_combinesOrderDataAndExternalInformation() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        stubRecipient();
        stubDestinationHub(DEST_HUB_ID);
        when(hubRouteQueryPort.findPath(
                eq(ORIGIN_HUB_ID), eq(DEST_HUB_ID), any()
        )).thenReturn(Optional.of(createValidRoutePath()));
        when(transactionService.create(any(DeliveryCreationData.class))).thenReturn(true);

        // when
        boolean result = deliveryCreationService.createDelivery(command);

        // then
        assertThat(result).isTrue();

        verify(hubRouteQueryPort).findPath(
                eq(ORIGIN_HUB_ID),
                eq(DEST_HUB_ID),
                eq(List.of(
                        new HubRouteProductInfo(PRODUCT_ID_1, 3),
                        new HubRouteProductInfo(PRODUCT_ID_2, 5),
                        new HubRouteProductInfo(PRODUCT_ID_3, 2)
                ))
        );

        ArgumentCaptor<DeliveryCreationData> creationDataCaptor =
                ArgumentCaptor.forClass(DeliveryCreationData.class);
        verify(transactionService).create(creationDataCaptor.capture());

        DeliveryCreationData creationData = creationDataCaptor.getValue();
        assertThat(creationData.traceId()).isEqualTo(TRACE_ID);
        assertThat(creationData.orderId()).isEqualTo(ORDER_ID);
        assertThat(creationData.supplierCompanyIds())
                .containsExactlyInAnyOrder(SUPPLIER_COMPANY_ID_1, SUPPLIER_COMPANY_ID_2);
        assertThat(creationData.recipientCompanyId()).isEqualTo(RECEIVER_COMPANY_ID);
        assertThat(creationData.originHubId()).isEqualTo(ORIGIN_HUB_ID);
        assertThat(creationData.destHubId()).isEqualTo(DEST_HUB_ID);
        assertThat(creationData.recipientName()).isEqualTo("홍길동");
        assertThat(creationData.recipientSlackId()).isEqualTo("U-RECIPIENT");
        assertThat(creationData.products()).containsExactly(
                new DeliveryCreationData.ProductData(PRODUCT_ID_1, 3),
                new DeliveryCreationData.ProductData(PRODUCT_ID_2, 5),
                new DeliveryCreationData.ProductData(PRODUCT_ID_3, 2)
        );
        assertThat(creationData.routes()).containsExactly(
                new DeliveryCreationData.RouteData(1, ORIGIN_HUB_ID, MIDDLE_HUB_ID, 120_000L, 5_400L),
                new DeliveryCreationData.RouteData(2, MIDDLE_HUB_ID, DEST_HUB_ID, 180_000L, 7_200L)
        );
    }

    @Test
    @DisplayName("출발 허브와 목적지 허브가 같으면 허브 경로를 조회하지 않는다")
    void createDelivery_sameOriginAndDestinationHub_skipsRouteLookup() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        stubRecipient();
        stubDestinationHub(ORIGIN_HUB_ID);
        when(transactionService.create(any(DeliveryCreationData.class))).thenReturn(true);

        // when
        boolean result = deliveryCreationService.createDelivery(command);

        // then
        assertThat(result).isTrue();
        verifyNoInteractions(hubRouteQueryPort);

        ArgumentCaptor<DeliveryCreationData> creationDataCaptor =
                ArgumentCaptor.forClass(DeliveryCreationData.class);
        verify(transactionService).create(creationDataCaptor.capture());
        assertThat(creationDataCaptor.getValue().routes()).isEmpty();
    }

    @Test
    @DisplayName("주문 상품 수량이 유효하지 않으면 외부 정보를 조회하지 않는다")
    void createDelivery_invalidOrderItem_throwsInvalidRequest() {
        // given
        CreateDeliveryCommand command = new CreateDeliveryCommand(
                TRACE_ID, ORDER_ID, ORIGIN_HUB_ID, RECEIVER_COMPANY_ID, RECEIVER_ID,
                "서울특별시 중구 세종대로 1", DELIVERY_DEADLINE, "도착 전 연락",
                List.of(new CreateDeliveryItemCommand(SUPPLIER_COMPANY_ID_1, PRODUCT_ID_1, 0))
        );

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryCreationService.createDelivery(command)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
        verifyNoInteractions(userQueryPort, companyQueryPort, hubRouteQueryPort, transactionService);
    }

    @Test
    @DisplayName("수령인을 찾을 수 없으면 배송 생성 실패 예외가 발생한다")
    void createDelivery_recipientNotFound_throwsReceiverNotFound() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        when(userQueryPort.findUser(RECEIVER_ID)).thenReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryCreationService.createDelivery(command)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(DeliveryCreationKafkaErrorCode.RECEIVER_NOT_FOUND);
        verifyNoInteractions(companyQueryPort, hubRouteQueryPort, transactionService);
    }

    @Test
    @DisplayName("수령 업체의 목적지 허브를 찾을 수 없으면 배송 생성 실패 예외가 발생한다")
    void createDelivery_destinationHubNotFound_throwsDestinationHubNotFound() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        stubRecipient();
        when(companyQueryPort.findHubInfo(RECEIVER_COMPANY_ID)).thenReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryCreationService.createDelivery(command)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode())
                .isEqualTo(DeliveryCreationKafkaErrorCode.DESTINATION_HUB_NOT_FOUND);
        verifyNoInteractions(hubRouteQueryPort, transactionService);
    }

    @Test
    @DisplayName("출발 허브에서 목적지 허브까지 경로가 없으면 배송 생성 실패 예외가 발생한다")
    void createDelivery_hubRouteNotFound_throwsHubRouteNotFound() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        stubRecipient();
        stubDestinationHub(DEST_HUB_ID);
        when(hubRouteQueryPort.findPath(
                eq(ORIGIN_HUB_ID), eq(DEST_HUB_ID), any()
        )).thenReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryCreationService.createDelivery(command)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
        verify(transactionService, never()).create(any());
    }

    @Test
    @DisplayName("허브 경로의 순번과 연결이 올바르지 않으면 배송 생성 실패 예외가 발생한다")
    void createDelivery_invalidRoutePath_throwsHubRouteNotFound() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        HubRoutePathInfo invalidPath = new HubRoutePathInfo(
                ORIGIN_HUB_ID,
                DEST_HUB_ID,
                List.of(new HubRoutePathInfo.RouteInfo(
                        UUID.randomUUID(), 2, ORIGIN_HUB_ID, DEST_HUB_ID, 300_000L, 12_600L
                ))
        );
        stubRecipient();
        stubDestinationHub(DEST_HUB_ID);
        when(hubRouteQueryPort.findPath(
                eq(ORIGIN_HUB_ID), eq(DEST_HUB_ID), any()
        )).thenReturn(Optional.of(invalidPath));

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryCreationService.createDelivery(command)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(DeliveryCreationKafkaErrorCode.HUB_ROUTE_NOT_FOUND);
        verify(transactionService, never()).create(any());
    }

    @Test
    @DisplayName("배송 저장 중 오류가 발생하면 배송 저장 실패 예외로 변환한다")
    void createDelivery_transactionFailure_throwsDeliverySaveFailed() {
        // given
        CreateDeliveryCommand command = createValidCommand(ORIGIN_HUB_ID);
        stubRecipient();
        stubDestinationHub(DEST_HUB_ID);
        when(hubRouteQueryPort.findPath(
                eq(ORIGIN_HUB_ID), eq(DEST_HUB_ID), any()
        )).thenReturn(Optional.of(createValidRoutePath()));
        when(transactionService.create(any(DeliveryCreationData.class)))
                .thenThrow(new RuntimeException("database error"));

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> deliveryCreationService.createDelivery(command)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(DeliveryCreationKafkaErrorCode.DELIVERY_SAVE_FAILED);
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    }

    // 정상 주문 생성 이벤트에 포함되는 상품 및 배송 정보를 구성합니다.
    private CreateDeliveryCommand createValidCommand(UUID originHubId) {
        return new CreateDeliveryCommand(
                TRACE_ID,
                ORDER_ID,
                originHubId,
                RECEIVER_COMPANY_ID,
                RECEIVER_ID,
                "서울특별시 중구 세종대로 1",
                DELIVERY_DEADLINE,
                "도착 전 연락",
                List.of(
                        new CreateDeliveryItemCommand(SUPPLIER_COMPANY_ID_1, PRODUCT_ID_1, 3),
                        new CreateDeliveryItemCommand(SUPPLIER_COMPANY_ID_2, PRODUCT_ID_2, 5),
                        new CreateDeliveryItemCommand(SUPPLIER_COMPANY_ID_1, PRODUCT_ID_3, 2)
                )
        );
    }

    // 정상 수령인 조회 결과를 구성합니다.
    private void stubRecipient() {
        UserInfo recipient = new UserInfo(
                RECEIVER_ID,
                "홍길동",
                "COMPANY_MANAGER",
                "APPROVED",
                "U-RECIPIENT",
                RECEIVER_COMPANY_ID,
                "COMPANY"
        );
        when(userQueryPort.findUser(RECEIVER_ID)).thenReturn(Optional.of(recipient));
    }

    // 수령 업체가 소속된 목적지 허브 조회 결과를 구성합니다.
    private void stubDestinationHub(UUID destHubId) {
        when(companyQueryPort.findHubInfo(RECEIVER_COMPANY_ID))
                .thenReturn(Optional.of(new CompanyHubInfo(RECEIVER_COMPANY_ID, destHubId)));
    }

    // 출발 허브부터 목적지 허브까지 이어지는 정상 경로를 구성합니다.
    private HubRoutePathInfo createValidRoutePath() {
        return new HubRoutePathInfo(
                ORIGIN_HUB_ID,
                DEST_HUB_ID,
                List.of(
                        new HubRoutePathInfo.RouteInfo(
                                UUID.randomUUID(), 1, ORIGIN_HUB_ID, MIDDLE_HUB_ID, 120_000L, 5_400L
                        ),
                        new HubRoutePathInfo.RouteInfo(
                                UUID.randomUUID(), 2, MIDDLE_HUB_ID, DEST_HUB_ID, 180_000L, 7_200L
                        )
                )
        );
    }
}
