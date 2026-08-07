package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.order.application.command.OrderCreateServiceCommand;
import com.sixro.logistics.order.application.command.OrderCreateServiceItem;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.entity.order.OrderItem;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

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

        Field field = Order.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(savedOrder, orderId);

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        when(orderRepository.saveAllOrderItems(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderCreateResult result =
                orderService.createOrder(command);

        // then
        verify(orderRepository, times(1))
                .save(any(Order.class));

        ArgumentCaptor<List<OrderItem>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(orderRepository)
                .saveAllOrderItems(captor.capture());

        List<OrderItem> savedItems = captor.getValue();

        assertThat(savedItems).hasSize(1);

        OrderItem item = savedItems.getFirst();

        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo("콜라");
        assertThat(item.getProductPrice()).isEqualTo(1500);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getCompanyId()).isEqualTo(companyId);

        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.hubId()).isEqualTo(hubId);
        assertThat(result.receiverCompanyId()).isEqualTo(receiverCompanyId);

        assertThat(result.orderItems()).hasSize(1);

        assertThat(result.orderItems().getFirst().productId())
                .isEqualTo(productId);

        assertThat(result.orderItems().getFirst().quantity())
                .isEqualTo(3);
    }
}
