package com.sixro.logistics.order.infrastructure.persistence.order;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.order.application.command.OrderSearchCommand;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.domain.entity.order.Order;
import com.sixro.logistics.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.sixro.logistics.order.domain.entity.order.QOrder.order;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Order> findAll(
            UUID userId, UserRole userRole, UUID affiliationId,
            OrderSearchCommand command, Pageable pageable
    ) {

        List<Order> content = queryFactory
                .selectFrom(order)
                .where(
                        roleCondition(userId, userRole, affiliationId),
                        hubIdEq(command.hubId()),
                        companyIdEq(command.receiverCompanyId()),
                        order.isDeleted.isFalse()
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .where(
                        roleCondition(userId, userRole, affiliationId),
                        hubIdEq(command.hubId()),
                        companyIdEq(command.receiverCompanyId()),
                        order.isDeleted.isFalse()
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0
        );
    }

    private BooleanExpression roleCondition(
            UUID userId, UserRole userRole, UUID affiliationId
    ){
        return switch(userRole){
            case MASTER_ADMIN -> null;

            case HUB_ADMIN ->
                    order.hubId.eq(affiliationId);

            case COMPANY_MANAGER ->
                    order.receiverCompanyId.eq(affiliationId)
                            .or(order.items.any().companyId.eq(affiliationId));

            case DELIVERY_MANAGER ->
                    throw new BaseException(CommonErrorCode.FORBIDDEN);
        };
    }

    private BooleanExpression hubIdEq(UUID hubId){
        return hubId != null
                ? order.hubId.eq(hubId) : null;
    }

    private BooleanExpression companyIdEq(UUID companyId){
        return companyId != null
                ? order.receiverCompanyId.eq(companyId) : null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        return pageable.getSort()
                .stream()
                .map(sortOrder -> {
                    com.querydsl.core.types.Order direction =
                            sortOrder.isAscending()
                                    ? com.querydsl.core.types.Order.ASC
                                    : com.querydsl.core.types.Order.DESC;

                    return switch (sortOrder.getProperty()) {
                        case "createdAt" -> new OrderSpecifier<>(direction, order.createdAt);

                        case "updatedAt" -> new OrderSpecifier<>(direction, order.updatedAt);

                        default -> throw new BaseException(
                                OrderErrorCode.INVALID_SORT_FIELD
                        );
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }

}
