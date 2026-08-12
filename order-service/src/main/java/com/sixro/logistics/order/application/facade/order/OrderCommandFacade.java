package com.sixro.logistics.order.application.facade.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.order.application.command.*;
import com.sixro.logistics.order.application.model.*;
import com.sixro.logistics.order.application.port.*;
import com.sixro.logistics.order.application.result.*;
import com.sixro.logistics.order.application.service.order.OrderCommandService;
import com.sixro.logistics.order.application.service.order.OrderQueryService;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.entity.order.OrderStatus;
import com.sixro.logistics.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCommandFacade {

    private final HubQueryPort hubQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final ProductQueryPort productQueryPort;
    private final InventoryCommandPort inventoryCommandPort;

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;

    public OrderCreateResult createOrder(
            UUID userId,
            UserRole userRole,
            OrderCreateCommand command
    ) {

        validateDuplicateProductIds(command.orderItems());

        // TO DO: 회원, 허브, 업체, 상품 서비스 호출 및 검증 (validate 호출)

        List<InventoryCommandItem> inventoryItems =
                command.orderItems()
                        .stream()
                        .map(item -> new InventoryCommandItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList();

        inventoryCommandPort.deductInventory(
                command.hubId(),
                inventoryItems
        );

        try{
            List<OrderCreateServiceItem> serviceItems =
                    createServiceItems(command);

            OrderCreateServiceCommand serviceCommand =
                    createServiceCommand(
                            command,
                            serviceItems
                    );

            return orderCommandService.createOrder(serviceCommand);

        }catch (Exception e){

            inventoryCommandPort.restoreInventory(
                    command.hubId(),
                    inventoryItems
            );

            throw e;
        }
    }

    public OrderUpdateResult updateOrder(UUID userId, UserRole userRole, UUID affiliationId,
                                         UUID orderId, OrderUpdateCommand command) {

        OrderGetOneResult result = orderQueryService.getOneOrder(orderId);

        // outbox에 order_confirmed 있는지 확인 필요할듯
        if(result.orderStatus() != OrderStatus.CREATED){
            throw new BaseException(OrderErrorCode.ORDER_CANNOT_BE_MODIFIED);
        }

        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        return orderCommandService.updateOrder(orderId, command);
    }

    public OrderCancelResult cancelOrder(
            UUID userId, UserRole userRole, UUID affiliationId, UUID orderId
    ) {

        OrderGetOneResult result = orderQueryService.getOneOrder(orderId);

        if(result.orderStatus() != OrderStatus.CREATED){
            throw new BaseException(OrderErrorCode.ORDER_CANNOT_BE_MODIFIED);
        }

        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        return orderCommandService.cancelOrder(orderId);
    }

    public OrderDeleteResult deleteOrder(UUID userId, UserRole userRole, UUID affiliationId,
                                         UUID orderId) {

        OrderGetOneResult result = orderQueryService.getOneOrder(orderId);

        if(result.orderStatus() != OrderStatus.CREATED){
            throw new BaseException(OrderErrorCode.ORDER_CANNOT_BE_MODIFIED);
        }

        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        return orderCommandService.deleteOrder(orderId);
    }

    public void deliveryCreated(DeliveryCreatedCommand command) {
        orderCommandService.deliveryCreated(command);
    }

    public void deliveryCreationFailed(DeliveryCreationFailedCommand command) {
        orderCommandService.deliveryCreationFailed(command);
    }

    private void validateDuplicateProductIds(
            List<OrderCommandItem> orderItems
    ) {
        Set<UUID> productIds = new HashSet<>();

        for (OrderCommandItem item : orderItems) {
            if (!productIds.add(item.productId())) {
                throw new BaseException(OrderErrorCode.DUPLICATE_PRODUCT);
            }
        }
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
            OrderCreateCommand command,
            List<OrderCreateServiceItem> items
    ) {
        return new OrderCreateServiceCommand(
                command.hubId(),
                command.receiverId(),
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
    ){

        if (command.orderItems().size() != productInfos.size()) {
            throw new BaseException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }

        if (command.orderItems().size() != inventoryInfo.inventories().size()) {
            throw new BaseException(OrderErrorCode.INVENTORY_NOT_FOUND);
        }

        Map<UUID, InventoryItemInfo> inventoryMap =
                inventoryInfo.inventories().stream()
                        .collect(Collectors.toMap(
                                InventoryItemInfo::productId,
                                Function.identity()
                        ));

        for (OrderCommandItem item : command.orderItems()) {

            InventoryItemInfo inventory =
                    inventoryMap.get(item.productId());

            if (inventory.stock() < item.quantity()) {
                throw new BaseException(OrderErrorCode.OUT_OF_STOCK);
            }
        }

    }
}
