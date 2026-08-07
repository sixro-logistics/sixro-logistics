package com.sixro.logistics.order.application.facade;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.order.application.command.OrderCommandItem;
import com.sixro.logistics.order.application.command.OrderCreateCommand;
import com.sixro.logistics.order.application.model.CompanyInfo;
import com.sixro.logistics.order.application.model.HubInfo;
import com.sixro.logistics.order.application.model.InventoryInfo;
import com.sixro.logistics.order.application.model.ProductInfo;
import com.sixro.logistics.order.application.port.*;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.application.service.OrderService;
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
            UUID userId, UserRole userRole, OrderCreateCommand command) {

        HubInfo hubInfo = hubQueryPort.getHub(command.hubId());
        CompanyInfo receiverCompanyInfo
                = companyQueryPort.getCompany(command.receiverCompanyId());

        List<UUID> productIds = command.orderItems().stream()
                .map(OrderCommandItem::productId)
                .toList();

        List<ProductInfo> productInfos = productQueryPort.getProducts(productIds);

        List<InventoryInfo> inventoryInfos
                = inventoryQueryPort.getInventories(command.hubId(), productIds);

        validate(command, hubInfo, receiverCompanyInfo, productInfos, inventoryInfos);

        return orderService.createOrder(command);
    }

    private void validate(OrderCreateCommand command,
                          HubInfo hubInfo, CompanyInfo companyInfo,
                          List<ProductInfo> productInfo, List<InventoryInfo> inventoryInfos) {

        // 생각해보니까 이건 OrderCreateFacade로 클래스명 변경할 수도

        // 상품이 모두 존재하고 삭제되지 않은 상태여야 함
        if(command.orderItems().size() != productInfo.size()){
            throw new BaseException(OrderErrorCode.PRODUCT_NOT_FOUND);
        }

        // 상품들이 모두 동일한 허브에 소속되어 있어야 함
        if (inventoryInfos.stream()
                .map(InventoryInfo::hubId)
                .distinct()
                .count() > 1) {
            throw new BaseException(OrderErrorCode.DIFFERENT_HUB_PRODUCT);
        }

        // 해당 허브에 상품의 재고가 모두 존재하고 삭제되지 않은 상태여야 함
        if(command.orderItems().size() != inventoryInfos.size()){
            throw new BaseException(OrderErrorCode.INVENTORY_NOT_FOUND);
        }

        // 허브의 해당 상품 재고가 주문 수량 이상이어야 함
        Map<UUID, InventoryInfo> inventoryMap = inventoryInfos.stream()
                .collect(Collectors.toMap(
                        InventoryInfo::productId,
                        Function.identity()
                ));

        for (OrderCommandItem item : command.orderItems()) {
            InventoryInfo inventory = inventoryMap.get(item.productId());

            if (inventory == null || inventory.stock() < item.quantity()) {
                throw new BaseException(OrderErrorCode.OUT_OF_STOCK);
            }
        }

    }

}
