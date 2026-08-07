package com.sixro.logistics.order.application.facade.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.command.OrderCommandItem;
import com.sixro.logistics.order.application.command.OrderCreateCommand;
import com.sixro.logistics.order.application.command.OrderCreateServiceCommand;
import com.sixro.logistics.order.application.command.OrderCreateServiceItem;
import com.sixro.logistics.order.application.mapper.order.OrderEventMapper;
import com.sixro.logistics.order.application.model.*;
import com.sixro.logistics.order.application.port.*;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.application.service.order.OrderService;
import com.sixro.logistics.order.application.service.outbox.OutboxService;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.event.order.OrderCreatedEvent;
import com.sixro.logistics.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final HubQueryPort hubQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final ProductQueryPort productQueryPort;
    private final InventoryQueryPort inventoryQueryPort;

    private final OrderService orderService;
    private final OutboxService outboxService;

    public OrderCreateResult createOrder(
            UUID userId, UserRole userRole, OrderCreateCommand command) {

        // TO DO: 허브, 업체, 상품 서비스 API 호출
        /*HubInfo hubInfo = hubQueryPort.getHub(command.hubId());
        CompanyInfo receiverCompanyInfo
                = companyQueryPort.getCompany(command.receiverCompanyId());*/

        List<UUID> productIds = command.orderItems().stream()
                .map(OrderCommandItem::productId)
                .toList();

        /*List<ProductInfo> productInfos = productQueryPort.getProducts(productIds);*/

        InventoryInfo inventoryInfo
                = inventoryQueryPort.getInventories(command.hubId(), productIds);

        //validate(command, hubInfo, receiverCompanyInfo, productInfos, inventoryInfo);

        /*Map<UUID, ProductInfo> productMap = productInfos.stream()
                .collect(Collectors.toMap(
                        ProductInfo::productId,
                        Function.identity()
                ));

        List<OrderCreateServiceItem> items =
                command.orderItems().stream()
                        .map(item -> {
                            ProductInfo product = productMap.get(item.productId());

                            return new OrderCreateServiceItem(
                                    item.productId(),
                                    product.productName(),
                                    product.price(),
                                    product.companyId(),
                                    item.quantity()
                            );
                        })
                        .toList();*/

        List<OrderCreateServiceItem> items =
                command.orderItems().stream()
                        .map(item -> {

                            return new OrderCreateServiceItem(
                                    item.productId(),
                                    "테스트 상품",
                                    15000,
                                    UUID.randomUUID(),
                                    item.quantity()
                            );
                        })
                        .toList();

        OrderCreateServiceCommand serviceCommand =
                new OrderCreateServiceCommand(
                        command.hubId(),
                        userId,
                        command.receiverCompanyId(),
                        "배송 주소",
                        command.deliveryDeadline(),
                        command.requests(),
                        items
                );

        OrderCreateResult result = orderService.createOrder(serviceCommand);

        OrderCreatedEvent event =
                OrderEventMapper.toEvent(result);

        outboxService.save(event);

        return result;
    }

    private void validate(OrderCreateCommand command,
                          HubInfo hubInfo, CompanyInfo companyInfo,
                          List<ProductInfo> productInfo, InventoryInfo inventoryInfo) {

        // 상품이 모두 존재하고 삭제되지 않은 상태여야 함
        if(command.orderItems().size() != productInfo.size()){
            throw new BaseException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }

        // 해당 허브에 상품의 재고가 모두 존재하고 삭제되지 않은 상태여야 함
        if(command.orderItems().size() != inventoryInfo.inventories().size()){
            throw new BaseException(OrderErrorCode.INVENTORY_NOT_FOUND);
        }

        // 허브의 해당 상품 재고가 주문 수량 이상이어야 함
        Map<UUID, InventoryItemInfo> inventoryMap = inventoryInfo.inventories().stream()
                .collect(Collectors.toMap(
                        InventoryItemInfo::productId,
                        Function.identity()
                ));

        for (OrderCommandItem item : command.orderItems()) {
            InventoryItemInfo inventory = inventoryMap.get(item.productId());

            if (inventory.stock() < item.quantity()) {
                throw new BaseException(OrderErrorCode.OUT_OF_STOCK);
            }
        }

    }

}
