package com.sixro.logistics.inventory.application.facade;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.*;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.domain.event.OrderCreatedItem;
import com.sixro.logistics.inventory.application.model.ProductInfo;
import com.sixro.logistics.inventory.application.port.HubQueryPort;
import com.sixro.logistics.inventory.application.port.ProductQueryPort;
import com.sixro.logistics.inventory.application.result.*;
import com.sixro.logistics.inventory.application.service.inventory.InventoryCommandService;
import com.sixro.logistics.inventory.application.service.inventory.InventoryQueryService;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryCommandFacade {

    private final InventoryCommandService inventoryCommandService;
    private final InventoryQueryService inventoryQueryService;
    private final HubQueryPort hubQueryPort;
    private final ProductQueryPort productQueryPort;

    public InventoryCreateResult createInventory(
            UUID userId, UserRole userRole, UUID affiliationId,
            InventoryCreateCommand command
    ) {

        // UserRole 검증
        if(userRole == UserRole.DELIVERY_MANAGER){
            throw new BaseException(InventoryErrorCode.FORBIDDEN);
        }

        // 허브 관리자 권한 검증
        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(command.hubId())){
                throw new BaseException(InventoryErrorCode.FORBIDDEN);
            }
        }

        /*HubInfo hubInfo = hubQueryPort.getHub(command.hubId());
        ProductInfo productInfo = productQueryPort.getProduct(command.productId());
        createValidate(hubInfo, productInfo);*/

        ProductInfo productInfo = new ProductInfo(UUID.randomUUID(), UUID.randomUUID());

        // 업체 관리자 권한 검증
        if(userRole == UserRole.COMPANY_MANAGER){
            if(!affiliationId.equals(productInfo.companyId())){
                throw new BaseException(InventoryErrorCode.FORBIDDEN);
            }
        }

        InventoryCreateServiceCommand serviceCommand
                = createServiceCommand(command, productInfo);

        return inventoryCommandService.createInventory(serviceCommand);
    }

    public InventoryUpdateResult updateInventory(
            UUID userId, UserRole userRole, UUID affiliationId,
            UUID inventoryId, InventoryUpdateCommand command
    ) {

        InventoryGetOneResult result = inventoryQueryService.getOneInventory(inventoryId);

        // 허브 관리자 권한 검증
        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(InventoryErrorCode.FORBIDDEN);
            }
        }

        return inventoryCommandService.updateInventory(inventoryId, command);
    }

    public InventoryStockInCompanyResult stockInInventory(
            UUID userId, UserRole userRole, UUID affiliationId,
            UUID inventoryId, InventoryStockInCommand command
    ) {

        InventoryGetOneResult result = inventoryQueryService.getOneInventory(inventoryId);

        // UserRole 검증
        if(userRole == UserRole.DELIVERY_MANAGER){
            throw new BaseException(InventoryErrorCode.FORBIDDEN);
        }

        // 허브 관리자 권한 검증
        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(InventoryErrorCode.FORBIDDEN);
            }
        }

        // 업체 관리자 권한 검증
        if(userRole == UserRole.COMPANY_MANAGER){
            if(!affiliationId.equals(result.companyId())){
                throw new BaseException(InventoryErrorCode.FORBIDDEN);
            }
        }

        return inventoryCommandService.stockInInventory(inventoryId, command);
    }

    // TO DO : deleteInventory에 userId 같이 넘기기
    public InventoryDeleteResult deleteInventory(
            UUID userId, UserRole userRole, UUID affiliationId,
            UUID inventoryId
    ) {

        InventoryGetOneResult result = inventoryQueryService.getOneInventory(inventoryId);

        if(userRole == UserRole.COMPANY_MANAGER || userRole == UserRole.DELIVERY_MANAGER){
            throw new BaseException(InventoryErrorCode.FORBIDDEN);
        }

        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(InventoryErrorCode.FORBIDDEN);
            }
        }

        return inventoryCommandService.deleteInventory(inventoryId);
    }

    public void deductStock(UUID orderId, UUID hubId, List<OrderCreatedItem> items) {
        InventoryDeductCommand command = new InventoryDeductCommand(
                orderId,
                hubId,
                items
                        .stream()
                        .map(item
                                        -> new InventoryDeductItem(
                                                item.productId(),
                                                item.quantity()
                                            )
                        )
                        .toList()
        );

        inventoryCommandService.deductInventory(command);
    }

    private InventoryCreateServiceCommand createServiceCommand(
            InventoryCreateCommand command,
            ProductInfo productInfo
    ) {
        return new InventoryCreateServiceCommand(
                command.hubId(),
                productInfo.companyId(),
                command.productId(),
                command.stock()
        );
    }

}
