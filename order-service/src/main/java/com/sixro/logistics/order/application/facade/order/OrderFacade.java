package com.sixro.logistics.order.application.facade.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.command.OrderCommandItem;
import com.sixro.logistics.order.application.command.OrderCreateCommand;
import com.sixro.logistics.order.application.command.OrderCreateServiceCommand;
import com.sixro.logistics.order.application.command.OrderCreateServiceItem;
import com.sixro.logistics.order.application.model.*;
import com.sixro.logistics.order.application.port.*;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.application.service.order.OrderService;
import com.sixro.logistics.order.common.model.UserRole;
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

    public OrderCreateResult createOrder(
            UUID userId,
            UserRole userRole,
            OrderCreateCommand command
    ) {

        InventoryInfo inventoryInfo = getInventoryInfo(command);

        // TO DO: 허브, 업체, 상품 서비스 호출 및 검증

        List<OrderCreateServiceItem> serviceItems =
                createServiceItems(command);

        OrderCreateServiceCommand serviceCommand =
                createServiceCommand(
                        userId,
                        command,
                        serviceItems
                );

        return orderService.createOrder(serviceCommand);
    }

    private InventoryInfo getInventoryInfo(OrderCreateCommand command) {
        return inventoryQueryPort.getInventories(
                command.hubId(),
                extractProductIds(command)
        );
    }

    private List<UUID> extractProductIds(OrderCreateCommand command) {
        return command.orderItems().stream()
                .map(OrderCommandItem::productId)
                .toList();
    }

    private List<OrderCreateServiceItem> createServiceItems(OrderCreateCommand command) {
        return command.orderItems().stream()
                .map(item -> new OrderCreateServiceItem(
                        item.productId(),
                        "테스트 상품",
                        15000,
                        UUID.randomUUID(),
                        item.quantity()
                ))
                .toList();
    }

    private OrderCreateServiceCommand createServiceCommand(
            UUID userId,
            OrderCreateCommand command,
            List<OrderCreateServiceItem> items
    ) {
        return new OrderCreateServiceCommand(
                command.hubId(),
                userId,
                command.receiverCompanyId(),
                "배송 주소",
                command.deliveryDeadline(),
                command.requests(),
                items
        );
    }

    private void validate(
            OrderCreateCommand command,
            HubInfo hubInfo,
            CompanyInfo companyInfo,
            List<ProductInfo> productInfos,
            InventoryInfo inventoryInfo
    ) {

        if(command.orderItems().size() != productInfos.size()){
            throw new BaseException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }

        if(command.orderItems().size() != inventoryInfo.inventories().size()){
            throw new BaseException(OrderErrorCode.INVENTORY_NOT_FOUND);
        }

        Map<UUID, InventoryItemInfo> inventoryMap =
                inventoryInfo.inventories().stream()
                        .collect(Collectors.toMap(
                                InventoryItemInfo::productId,
                                Function.identity()
                        ));

        for(OrderCommandItem item : command.orderItems()){

            InventoryItemInfo inventory =
                    inventoryMap.get(item.productId());

            if(inventory.stock() < item.quantity()){
                throw new BaseException(OrderErrorCode.OUT_OF_STOCK);
            }
        }
    }
}
