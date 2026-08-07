package com.sixro.logistics.order.application.service;

import com.sixro.logistics.order.application.command.OrderCreateCommand;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.domain.entity.Order;
import com.sixro.logistics.order.domain.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderCreateResult createOrder(OrderCreateCommand command) {

        /*Order order = Order.create(command.hubId(), ordererId, command.receiverCompanyId(),
                deliveryAddress, command.deliveryDeadline(), command.requests());

        orderRepository.save(order);
        return new OrderCreateResult(order.getId(), order.getHubId(), order.getReceiverCompanyId(),
                deliveryAddress, deliveryDeadline, requests, orderStatus, orderItems);*/
        return null;
    }
}
