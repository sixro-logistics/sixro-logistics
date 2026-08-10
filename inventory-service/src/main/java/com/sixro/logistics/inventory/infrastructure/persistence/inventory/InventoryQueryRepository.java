package com.sixro.logistics.inventory.infrastructure.persistence.inventory;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.InventorySearchCommand;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.sixro.logistics.inventory.domain.entity.inventory.QInventory.inventory;

@Repository
@RequiredArgsConstructor
public class InventoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Inventory> findAll(
            UserRole userRole, UUID affiliationId,
            InventorySearchCommand command, Pageable pageable
    ) {

        List<Inventory> content = queryFactory
                .selectFrom(inventory)
                .where(
                        roleCondition(userRole, affiliationId),
                        hubIdEq(command.hubId()),
                        companyIdEq(command.companyId()),
                        productIdEq(command.productId()),
                        inventory.isDeleted.isFalse()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(inventory.count())
                .from(inventory)
                .where(
                        roleCondition(userRole, affiliationId),
                        hubIdEq(command.hubId()),
                        companyIdEq(command.companyId()),
                        productIdEq(command.productId()),
                        inventory.isDeleted.isFalse()
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0
        );
    }

    private BooleanExpression roleCondition(UserRole userRole, UUID affiliationId) {
        return switch(userRole){
            case MASTER_ADMIN
                    -> null;
            case HUB_ADMIN
                    -> inventory.hubId.eq(affiliationId);
            case COMPANY_MANAGER
                    -> inventory.companyId.eq(affiliationId);
            case DELIVERY_MANAGER -> throw new BaseException(
                    InventoryErrorCode.FORBIDDEN
            );
        };
    }

    private BooleanExpression hubIdEq(UUID hubId) {
        return hubId != null
                ? inventory.hubId.eq(hubId)
                : null;
    }

    private BooleanExpression companyIdEq(UUID companyId) {
        return companyId != null
                ? inventory.companyId.eq(companyId)
                : null;
    }

    private BooleanExpression productIdEq(UUID productId) {
        return productId != null
                ? inventory.productId.eq(productId)
                : null;
    }

}
