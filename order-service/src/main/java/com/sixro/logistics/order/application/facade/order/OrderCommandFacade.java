package com.sixro.logistics.order.application.facade.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.order.application.command.*;
import com.sixro.logistics.order.application.model.*;
import com.sixro.logistics.order.application.port.*;
import com.sixro.logistics.order.application.result.*;
import com.sixro.logistics.order.application.service.order.OrderCommandService;
import com.sixro.logistics.order.application.service.order.OrderIdempotencyService;
import com.sixro.logistics.order.application.service.order.OrderQueryService;
import com.sixro.logistics.order.application.service.order.OrderRequestHashGenerator;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.entity.order.OrderIdempotency;
import com.sixro.logistics.order.domain.entity.order.OrderIdempotencyStatus;
import com.sixro.logistics.order.domain.entity.order.OrderStatus;
import com.sixro.logistics.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCommandFacade {

    private final UserQueryPort userQueryPort;
    private final HubQueryPort hubQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final ProductQueryPort productQueryPort;
    private final InventoryCommandPort inventoryCommandPort;

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;

    private final OrderIdempotencyService orderIdempotencyService;
    private final OrderRequestHashGenerator orderRequestHashGenerator;

    public OrderCreateResult createOrder(OrderCreateCommand command) {

        // 동일한 상품은 하나로 묶어서 요청해야 함
        validateDuplicateProductIds(command.orderItems());

        UserInfo userInfo = userQueryPort.getUser(command.receiverId());
        if(userInfo == null){
            throw new BaseException(OrderErrorCode.RECEIVER_NOT_FOUND);
        }

        HubInfo hubInfo = hubQueryPort.getHub(command.hubId());
        if(hubInfo == null){
            throw new BaseException(OrderErrorCode.HUB_NOT_FOUND);
        }

        CompanyInfo companyInfo = companyQueryPort.getCompany(command.receiverCompanyId());
        if(companyInfo == null){
            throw new BaseException(OrderErrorCode.COMPANY_NOT_FOUND);
        }

        List<InventoryCommandItem> inventoryItems =
                command.orderItems()
                        .stream()
                        .map(item -> new InventoryCommandItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList();

        List<UUID> productIds = inventoryItems.stream()
                        .map(item -> item.productId())
                        .toList();

        List<ProductInfo> productInfo = productQueryPort.getProducts(productIds);
        if(productInfo.size() != inventoryItems.size()){
            throw new BaseException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }

        // 주문 생성 성공 후에만 멱등키를 기록하지 않고, 요청 처리 단계에 따라 상태를 관리
        String requestHash =
                orderRequestHashGenerator.generate(command);

        Optional<OrderIdempotency> existing =
                orderIdempotencyService.find(command.idempotencyKey());

        if(existing.isPresent()){
            OrderIdempotency idempotency = existing.get();

            // 같은 멱등키인데 다른 주문 요청
            if(!idempotency.getRequestHash().equals(requestHash)){
                throw new BaseException(OrderErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }

            // 이미 처리 중인 요청
            if(idempotency.getStatus() == OrderIdempotencyStatus.PROCESSING){
                throw new BaseException(OrderErrorCode.ORDER_ALREADY_PROCESSING);
            }

            // 실패 후 재고 복원까지 완료된 요청
            if(idempotency.getStatus() == OrderIdempotencyStatus.COMPENSATED){
                throw new BaseException(OrderErrorCode.IDEMPOTENCY_KEY_COMPENSATED);
            }

            // 이미 성공한 요청이면 기존 주문 반환
            if(idempotency.getStatus() == OrderIdempotencyStatus.SUCCEEDED){
                return orderCommandService.getExistingOrder(idempotency.getOrderId());
            }
        }

        orderIdempotencyService.start(
                command.idempotencyKey(),
                requestHash
        );

        // 중복 차감, 복원 방지하기 위해 멱등키를 재고 서비스로 전달
        UUID deductIdempotencyKey = command.idempotencyKey();

        UUID restoreIdempotencyKey = UUID.nameUUIDFromBytes(
                (command.idempotencyKey() + ":RESTORE")
                        .getBytes(StandardCharsets.UTF_8)
        );

        boolean inventoryDeducted = false;

        try{

            // 재고 차감 동기 처리
            inventoryCommandPort.deductInventory(
                    deductIdempotencyKey,
                    command.hubId(),
                    inventoryItems
            );

            inventoryDeducted = true;

            List<OrderCreateServiceItem> serviceItems =
                    createServiceItems(command, productInfo);

            OrderCreateServiceCommand serviceCommand =
                    createServiceCommand(
                            command,
                            companyInfo,
                            serviceItems
                    );

            return orderCommandService.createOrder(serviceCommand);

        }catch(Exception e){

            // 주문 생성 실패 시 재고 복원 동기 처리
            if(inventoryDeducted){
                inventoryCommandPort.restoreInventory(
                        restoreIdempotencyKey,
                        command.hubId(),
                        inventoryItems
                );

                // 재고 복원이 성공한 경우에만 멱등키 상태를 COMPENSATED로 변경
                orderIdempotencyService.compensate(command.idempotencyKey());
            }

            throw e;
        }
    }

    public OrderUpdateResult updateOrder(
            UserRole userRole, UUID affiliationId,
            UUID orderId, OrderUpdateCommand command
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

        return orderCommandService.updateOrder(orderId, command);
    }

    public OrderCancelResult cancelOrder(
            UserRole userRole, UUID affiliationId, UUID orderId
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

        return orderCommandService.deleteOrder(userId, orderId);
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

    private List<OrderCreateServiceItem> createServiceItems(
            OrderCreateCommand command,
            List<ProductInfo> productInfos
    ) {
        Map<UUID, ProductInfo> productInfoMap =
                productInfos.stream()
                        .collect(Collectors.toMap(
                                ProductInfo::productId,
                                Function.identity()
                        ));

        return command.orderItems().stream()
                .map(item -> {
                    ProductInfo product =
                            productInfoMap.get(item.productId());

                    return new OrderCreateServiceItem(
                            product.productId(),
                            product.productName(),
                            product.price(),
                            product.companyId(),
                            item.quantity()
                    );
                })
                .toList();
    }

    private OrderCreateServiceCommand createServiceCommand(
            OrderCreateCommand command,
            CompanyInfo companyInfo,
            List<OrderCreateServiceItem> items
    ) {
        return new OrderCreateServiceCommand(
                command.idempotencyKey(),
                command.hubId(),
                command.receiverId(),
                command.receiverCompanyId(),
                companyInfo.address(),
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
