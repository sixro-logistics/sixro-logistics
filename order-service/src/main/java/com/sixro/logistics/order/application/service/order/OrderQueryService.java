package com.sixro.logistics.order.application.service.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.command.OrderSearchCommand;
import com.sixro.logistics.order.application.result.OrderGetOneResult;
import com.sixro.logistics.order.application.result.OrderResultItem;
import com.sixro.logistics.order.application.result.OrderSearchItem;
import com.sixro.logistics.order.application.result.OrderSearchResult;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.domain.repository.order.OrderRepository;
import com.sixro.logistics.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderGetOneResult getOneOrder(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() ->
                        new BaseException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        return new OrderGetOneResult(
                order.getId(),
                order.getReceiverId(),
                order.getHubId(),
                order.getReceiverCompanyId(),
                order.getDeliveryAddress(),
                order.getDeliveryDeadline(),
                order.getRequests(),
                order.getOrderStatus(),
                order.getItems()
                        .stream()
                        .map(item -> new OrderResultItem(
                                item.getProductId(),
                                item.getProductName(),
                                item.getProductPrice(),
                                item.getQuantity(),
                                item.getCompanyId()
                        ))
                        .toList()
        );
    }

    public OrderSearchResult search(
            UUID userId, UserRole userRole, UUID affiliationId,
            OrderSearchCommand command, Pageable pageable
    ) {

        // TO DO: userRole 임의로 설정한 부분 삭제
        userRole = UserRole.MASTER_ADMIN;

        Page<Order> page = orderRepository.findAll(
                userId, userRole, affiliationId, command, pageable
        );

        Page<OrderSearchItem> resultPage = page
                .map(order -> new OrderSearchItem(
                        order.getId(),
                        order.getReceiverId(),
                        order.getHubId(),
                        order.getReceiverCompanyId(),
                        order.getDeliveryAddress(),
                        order.getDeliveryDeadline(),
                        order.getRequests(),
                        order.getOrderStatus()
                ));

        return new OrderSearchResult(resultPage);
    }
}
