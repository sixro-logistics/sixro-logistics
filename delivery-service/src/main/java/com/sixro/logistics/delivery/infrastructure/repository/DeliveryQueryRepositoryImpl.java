package com.sixro.logistics.delivery.infrastructure.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.DeliverySearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.sixro.logistics.delivery.domain.entity.QDelivery.delivery;
import static com.sixro.logistics.delivery.domain.entity.QDeliveryManager.deliveryManager;
import static com.sixro.logistics.delivery.domain.entity.QDeliveryRoute.deliveryRoute;

@Repository
@RequiredArgsConstructor
public class DeliveryQueryRepositoryImpl implements DeliveryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Delivery> search(DeliverySearchCondition condition, Pageable pageable) {
        List<Delivery> content = queryFactory
                .selectFrom(delivery)
                .leftJoin(delivery.deliveryManager, deliveryManager)
                .where(searchConditions(condition))
                .orderBy(resolveOrder(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(delivery.count())
                .from(delivery)
                .leftJoin(delivery.deliveryManager, deliveryManager)
                .where(searchConditions(condition))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private BooleanExpression[] searchConditions(DeliverySearchCondition condition) {
        return new BooleanExpression[]{
                delivery.isDeleted.isFalse(),
                orderIdEq(condition.orderId()),
                deliveryStatusEq(condition.deliveryStatus()),
                originHubIdEq(condition.originHubId()),
                destHubIdEq(condition.destHubId()),
                deliveryManagerIdEq(condition.deliveryManagerId()),
                deadlineBeforeOrEq(condition.deadline()),
                scopeCondition(condition)
        };
    }

    private BooleanExpression orderIdEq(UUID orderId) {
        return orderId == null ? null : delivery.orderId.eq(orderId);
    }

    private BooleanExpression deliveryStatusEq(DeliveryStatus deliveryStatus) {
        return deliveryStatus == null ? null : delivery.deliveryStatus.eq(deliveryStatus);
    }

    private BooleanExpression originHubIdEq(UUID originHubId) {
        return originHubId == null ? null : delivery.originHubId.eq(originHubId);
    }

    private BooleanExpression destHubIdEq(UUID destHubId) {
        return destHubId == null ? null : delivery.destHubId.eq(destHubId);
    }

    private BooleanExpression deliveryManagerIdEq(UUID deliveryManagerId) {
        return deliveryManagerId == null ? null : deliveryManager.deliveryManagerId.eq(deliveryManagerId);
    }

    private BooleanExpression deadlineBeforeOrEq(LocalDateTime deadline) {
        return deadline == null ? null : delivery.deliveryDeadline.loe(deadline);
    }

    private BooleanExpression scopeCondition(DeliverySearchCondition condition) {
        if (condition.searchScope() == null) {
            return Expressions.FALSE;
        }

        return switch (condition.searchScope()) {
            case ALL -> null;
            case RELATED_HUB -> relatedHub(condition.scopeId());
            case ASSIGNED_MANAGER -> assignedDeliveryManager(condition.scopeId());
            case RELATED_COMPANY -> relatedCompany(condition.scopeId());
        };
    }

    private BooleanExpression relatedHub(UUID affiliationId) {
        return affiliationId == null ?
                Expressions.FALSE : delivery.originHubId.eq(affiliationId).or(delivery.destHubId.eq(affiliationId));
    }

    private BooleanExpression assignedDeliveryManager(UUID loginUserId) {
        if (loginUserId == null) {
            return Expressions.FALSE;
        }

        BooleanExpression assignedCompanyDelivery = deliveryManager.deliveryManagerId.eq(loginUserId);
        BooleanExpression assignedHubRoute = JPAExpressions.selectOne()
                .from(deliveryRoute)
                .where(deliveryRoute.isDeleted.isFalse(), deliveryRoute.delivery.eq(delivery),
                        deliveryRoute.deliveryManager.deliveryManagerId.eq(loginUserId))
                .exists();

        return assignedCompanyDelivery.or(assignedHubRoute);
    }

    private BooleanExpression relatedCompany(UUID affiliationId) {
        return affiliationId == null ? Expressions.FALSE
                : delivery.supplierCompanyId.eq(affiliationId).or(delivery.recipientCompanyId.eq(affiliationId));
    }

    private OrderSpecifier<?> resolveOrder(Pageable pageable) {
        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(Sort.Order.desc("createdAt"));
        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

        return switch (sortOrder.getProperty()) {
            case "createdAt" -> new OrderSpecifier<>(direction, delivery.createdAt);
            case "updatedAt" -> new OrderSpecifier<>(direction, delivery.updatedAt);
            case "deliveryDeadline" -> new OrderSpecifier<>(direction, delivery.deliveryDeadline);
            default -> throw new BaseException(CommonErrorCode.INVALID_PARAMETER);
        };
    }
}
