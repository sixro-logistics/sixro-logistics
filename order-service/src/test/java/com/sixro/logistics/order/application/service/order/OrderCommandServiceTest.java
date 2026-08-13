package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.command.*;
import com.sixro.logistics.order.application.event.EventEnvelope;
import com.sixro.logistics.order.application.result.*;
import com.sixro.logistics.order.application.service.event.ProcessedEventService;
import com.sixro.logistics.order.application.service.outbox.OutboxService;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
import com.sixro.logistics.order.domain.entity.order.OrderStatus;
import com.sixro.logistics.order.domain.repository.order.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ProcessedEventService processedEventService;

    @InjectMocks
    private OrderCommandService orderService;

    private static final UUID SYSTEM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");


    @Test
    void createOrder_success() throws Exception {

        // given
        UUID orderId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID ordererId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        OrderCreateServiceCommand command =
                new OrderCreateServiceCommand(
                        hubId,
                        ordererId,
                        receiverCompanyId,
                        "서울시 강남구",
                        LocalDateTime.now().plusDays(1),
                        "문 앞",
                        List.of(
                                new OrderCreateServiceItem(
                                        productId,
                                        "콜라",
                                        1500,
                                        companyId,
                                        3
                                )
                        )
                );

        Order savedOrder = Order.create(
                hubId,
                ordererId,
                receiverCompanyId,
                "서울시 강남구",
                command.deliveryDeadline(),
                "문 앞"
        );

        setOrderId(savedOrder, orderId);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    setOrderId(order, orderId);
                    return order;
                });

        // when
        OrderCreateResult result =
                orderService.createOrder(command);

        // then
        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();

        List<OrderItem> savedItems =
                capturedOrder.getItems();

        assertThat(savedItems)
                .hasSize(1);

        OrderItem item = savedItems.getFirst();

        assertThat(item.getProductId())
                .isEqualTo(productId);

        assertThat(item.getProductName())
                .isEqualTo("콜라");

        assertThat(item.getProductPrice())
                .isEqualTo(1500);

        assertThat(item.getQuantity())
                .isEqualTo(3);

        assertThat(item.getCompanyId())
                .isEqualTo(companyId);

        assertThat(result.orderId())
                .isEqualTo(orderId);

        assertThat(result.hubId())
                .isEqualTo(hubId);

        assertThat(result.receiverCompanyId())
                .isEqualTo(receiverCompanyId);

        assertThat(result.orderItems())
                .hasSize(1);

        assertThat(result.orderItems().getFirst().productId())
                .isEqualTo(productId);

        assertThat(result.orderItems().getFirst().quantity())
                .isEqualTo(3);

        verify(outboxService)
                .saveOrderCreated(any(EventEnvelope.class));
    }


    @Test
    void updateOrder_success() {

        // given
        UUID orderId = UUID.randomUUID();

        Order order = Order.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                LocalDateTime.now().plusDays(1),
                "문 앞"
        );

        setOrderIdWithoutException(order, orderId);

        OrderUpdateCommand command =
                new OrderUpdateCommand(
                        LocalDateTime.now().plusDays(2),
                        "배송 전에 연락해주세요."
                );

        when(orderRepository.findByIdWithItems(orderId))
                .thenReturn(Optional.of(order));

        // when
        OrderUpdateResult result =
                orderService.updateOrder(orderId, command);

        // then
        assertThat(result.orderId())
                .isEqualTo(orderId);

        assertThat(result.deliveryDeadline())
                .isEqualTo(command.deliveryDeadline());

        assertThat(result.requests())
                .isEqualTo(command.requests());

        verify(orderRepository)
                .findByIdWithItems(orderId);
    }


    @Test
    void updateOrder_orderNotFound() {

        // given
        UUID orderId = UUID.randomUUID();

        OrderUpdateCommand command =
                new OrderUpdateCommand(
                        LocalDateTime.now().plusDays(1),
                        "문 앞"
                );

        when(orderRepository.findByIdWithItems(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderService.updateOrder(orderId, command)
        )
                .isInstanceOf(BaseException.class);

        verify(orderRepository)
                .findByIdWithItems(orderId);

        verifyNoInteractions(outboxService);
    }


    @Test
    void cancelOrder_success() {

        // given
        UUID orderId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order order = Order.create(
                hubId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                LocalDateTime.now().plusDays(1),
                "문 앞"
        );

        setOrderIdWithoutException(order, orderId);

        OrderItem item = OrderItem.create(
                productId,
                "콜라",
                1500,
                3,
                UUID.randomUUID()
        );

        order.addOrderItem(item);

        when(orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.of(order));

        // when
        OrderCancelResult result =
                orderService.cancelOrder(orderId);

        // then
        assertThat(result.orderId())
                .isEqualTo(orderId);

        assertThat(result.orderStatus())
                .isEqualTo(OrderStatus.CANCELED);

        verify(orderRepository)
                .findForUpdateByIdAndIsDeletedFalse(orderId);

        verify(outboxService)
                .saveOrderCanceled(any(EventEnvelope.class));
    }


    @Test
    void cancelOrder_orderNotFound() {

        // given
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderService.cancelOrder(orderId)
        )
                .isInstanceOf(BaseException.class);

        verify(orderRepository)
                .findForUpdateByIdAndIsDeletedFalse(orderId);

        verifyNoInteractions(outboxService);
    }


    @Test
    void deleteOrder_success() {

        // given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Order order = Order.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                LocalDateTime.now().plusDays(1),
                "문 앞"
        );

        setOrderIdWithoutException(order, orderId);

        when(orderRepository.findByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.of(order));

        // when
        OrderDeleteResult result =
                orderService.deleteOrder(userId, orderId);

        // then
        assertThat(result.orderId())
                .isEqualTo(orderId);

        verify(orderRepository)
                .findByIdAndIsDeletedFalse(orderId);

        verifyNoInteractions(outboxService);
    }


    @Test
    void deleteOrder_orderNotFound() {

        // given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderService.deleteOrder(userId, orderId)
        )
                .isInstanceOf(BaseException.class);

        verify(orderRepository)
                .findByIdAndIsDeletedFalse(orderId);

        verifyNoInteractions(outboxService);
    }


    @Test
    void deliveryCreated_success() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DeliveryCreatedCommand command =
                new DeliveryCreatedCommand(
                        eventId,
                        orderId,
                        UUID.randomUUID()
                );

        Order order = Order.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                LocalDateTime.now().plusDays(1),
                "문 앞"
        );

        setOrderIdWithoutException(order, orderId);

        when(processedEventService.isProcessed(eventId))
                .thenReturn(false);

        when(orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.of(order));

        // when
        orderService.deliveryCreated(command);

        // then
        assertThat(order.getOrderStatus())
                .isEqualTo(OrderStatus.DELIVERY_CREATED);

        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService)
                .save(eventId);
    }


    @Test
    void deliveryCreated_duplicateEvent() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DeliveryCreatedCommand command =
                new DeliveryCreatedCommand(
                        eventId,
                        orderId,
                        UUID.randomUUID()
                );

        when(processedEventService.isProcessed(eventId))
                .thenReturn(true);

        // when
        orderService.deliveryCreated(command);

        // then
        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService, never())
                .save(any(UUID.class));

        verifyNoInteractions(orderRepository);
    }


    @Test
    void deliveryCreated_orderNotFound() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DeliveryCreatedCommand command =
                new DeliveryCreatedCommand(
                        eventId,
                        orderId,
                        UUID.randomUUID()
                );

        when(processedEventService.isProcessed(eventId))
                .thenReturn(false);

        when(orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderService.deliveryCreated(command)
        )
                .isInstanceOf(BaseException.class);

        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService, never())
                .save(any(UUID.class));
    }


    @Test
    void deliveryCreationFailed_success() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Order order = Order.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울시 강남구",
                LocalDateTime.now().plusDays(1),
                "문 앞"
        );

        setOrderIdWithoutException(order, orderId);

        OrderItem item = OrderItem.create(
                UUID.randomUUID(),
                "콜라",
                1500,
                3,
                UUID.randomUUID()
        );

        order.addOrderItem(item);

        DeliveryCreationFailedCommand command =
                new DeliveryCreationFailedCommand(
                        eventId,
                        orderId
                );

        when(processedEventService.isProcessed(eventId))
                .thenReturn(false);

        when(orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.of(order));

        // when
        orderService.deliveryCreationFailed(command);

        // then
        assertThat(order.getOrderStatus())
                .isEqualTo(OrderStatus.FAILED);

        assertThat(order.getItems())
                .hasSize(1);

        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService)
                .save(eventId);

        verify(outboxService)
                .saveOrderFailed(any(EventEnvelope.class));
    }


    @Test
    void deliveryCreationFailed_duplicateEvent() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DeliveryCreationFailedCommand command =
                new DeliveryCreationFailedCommand(
                        eventId,
                        orderId
                );

        when(processedEventService.isProcessed(eventId))
                .thenReturn(true);

        // when
        orderService.deliveryCreationFailed(command);

        // then
        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService, never())
                .save(any(UUID.class));

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(outboxService);
    }


    @Test
    void deliveryCreationFailed_orderNotFound() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DeliveryCreationFailedCommand command =
                new DeliveryCreationFailedCommand(
                        eventId,
                        orderId
                );

        when(processedEventService.isProcessed(eventId))
                .thenReturn(false);

        when(orderRepository.findForUpdateByIdAndIsDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderService.deliveryCreationFailed(command)
        )
                .isInstanceOf(BaseException.class);

        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService, never())
                .save(any(UUID.class));

        verifyNoInteractions(outboxService);
    }


    private void setOrderId(
            Order order,
            UUID orderId
    ) throws Exception {

        Field field = Order.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(order, orderId);
    }


    private void setOrderIdWithoutException(
            Order order,
            UUID orderId
    ) {

        try {
            setOrderId(order, orderId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}