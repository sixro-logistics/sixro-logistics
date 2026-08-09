package com.sixro.logistics.inventory.application.facade;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.InventoryCheckCommand;
import com.sixro.logistics.inventory.application.command.InventorySearchCommand;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.application.result.InventoryCheckResult;
import com.sixro.logistics.inventory.application.result.InventoryGetOneResult;
import com.sixro.logistics.inventory.application.result.InventorySearchResult;
import com.sixro.logistics.inventory.application.service.InventoryQueryService;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

import static com.sixro.logistics.common.core.exception.CommonErrorCode.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class InventoryQueryFacade {

    private final InventoryQueryService inventoryQueryService;

    public InventoryCheckResult checkInventory(InventoryCheckCommand command) {
        return inventoryQueryService.checkInventory(command);
    }

    public InventoryGetOneResult getOneInventory(
            UUID userId, UserRole userRole, UUID affiliationId, UUID inventoryId
    ) {

        // UserRole 검증
        if(userRole == UserRole.DELIVERY_MANAGER){
            throw new BaseException(InventoryErrorCode.FORBIDDEN);
        }

        InventoryGetOneResult result = inventoryQueryService.getOneInventory(inventoryId);

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

        return result;
    }

    public InventorySearchResult searchInventory(
            UserRole userRole, UUID affiliationId,
            InventorySearchCommand command, Pageable pageable
    ) {

        // UserRole 검증
        if(userRole == UserRole.DELIVERY_MANAGER){
            throw new BaseException(InventoryErrorCode.FORBIDDEN);
        }

        Pageable validatedPageable = pageValidate(pageable);

        return inventoryQueryService.searchInventory(
                userRole,
                affiliationId,
                command,
                validatedPageable
        );
    }

    private Pageable pageValidate(Pageable pageable){
        Set<String> sortList = Set.of("createdAt", "updatedAt");

        for(Sort.Order order : pageable.getSort()){
            if(!sortList.contains(order.getProperty())){
                throw new BaseException(InventoryErrorCode.INVALID_SORT_FIELD);
            }
        }

        int size = pageable.getPageSize();
        if(size != 10 && size != 30 && size != 50){
            return PageRequest.of(
                    pageable.getPageNumber(),
                    10,
                    pageable.getSort()
            );
        }

        return pageable;
    }

}
