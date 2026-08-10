package com.sixro.logistics.delivery.infrastructure.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.entity.QDeliveryManager;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.sixro.logistics.delivery.domain.entity.QDelivery.delivery;
import static com.sixro.logistics.delivery.domain.entity.QDeliveryRoute.deliveryRoute;

@Repository
@RequiredArgsConstructor
public class DeliveryRouteQueryRepositoryImpl implements DeliveryRouteQueryRepository {

    private static final QDeliveryManager routeManager = new QDeliveryManager("routeManager");
    private static final QDeliveryManager finalDeliveryManager = new QDeliveryManager("finalDeliveryManager");

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DeliveryRoute> search(DeliveryRouteSearchCondition condition, Pageable pageable) {
        List<DeliveryRoute> content = queryFactory
                .selectFrom(deliveryRoute)
                .join(deliveryRoute.delivery, delivery)
                .leftJoin(deliveryRoute.deliveryManager, routeManager)
                .leftJoin(delivery.deliveryManager, finalDeliveryManager)
                .where(searchConditions(condition))
                .orderBy(resolveOrder(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(deliveryRoute.count())
                .from(deliveryRoute)
                .join(deliveryRoute.delivery, delivery)
                .leftJoin(deliveryRoute.deliveryManager, routeManager)
                .leftJoin(delivery.deliveryManager, finalDeliveryManager)
                .where(searchConditions(condition))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private BooleanExpression[] searchConditions(DeliveryRouteSearchCondition condition) {
        return new BooleanExpression[]{
                deliveryRoute.isDeleted.isFalse(),
                deliveryIdEq(condition.deliveryId()),
                routeStatusEq(condition.routeStatus()),
                originHubIdEq(condition.originHubId()),
                destHubIdEq(condition.destHubId()),
                deliveryManagerIdEq(condition.deliveryManagerId()),
                routeSequenceEq(condition.routeSequence()),
                scopeCondition(condition)
        };
    }

    private BooleanExpression deliveryIdEq(UUID deliveryId) {
        return deliveryId == null ? null : delivery.deliveryId.eq(deliveryId);
    }

    private BooleanExpression routeStatusEq(RouteStatus routeStatus) {
        return routeStatus == null ? null : deliveryRoute.routeStatus.eq(routeStatus);
    }

    private BooleanExpression originHubIdEq(UUID originHubId) {
        return originHubId == null ? null : deliveryRoute.originHubId.eq(originHubId);
    }

    private BooleanExpression destHubIdEq(UUID destHubId) {
        return destHubId == null ? null : deliveryRoute.destHubId.eq(destHubId);
    }

    private BooleanExpression deliveryManagerIdEq(UUID deliveryManagerId) {
        return deliveryManagerId == null ? null : routeManager.deliveryManagerId.eq(deliveryManagerId);
    }

    private BooleanExpression routeSequenceEq(Integer routeSequence) {
        return routeSequence == null ? null : deliveryRoute.routeSequence.eq(routeSequence);
    }

    private BooleanExpression scopeCondition(DeliveryRouteSearchCondition condition) {
        if (condition.searchScope() == null) {
            return Expressions.FALSE;
        }

        return switch (condition.searchScope()) {
            case ALL -> null;
            case RELATED_HUB -> relatedHub(condition.scopeId());
            case ASSIGNED_MANAGER -> assignedManager(condition.scopeId());
            case RELATED_COMPANY -> relatedCompany(condition.scopeId());
        };
    }

    private BooleanExpression relatedHub(UUID hubId) {
        return hubId == null ? Expressions.FALSE
                : deliveryRoute.originHubId.eq(hubId).or(deliveryRoute.destHubId.eq(hubId));
    }

    private BooleanExpression assignedManager(UUID deliveryManagerId) {
        return deliveryManagerId == null ? Expressions.FALSE
                : routeManager.deliveryManagerId.eq(deliveryManagerId)
                .or(finalDeliveryManager.deliveryManagerId.eq(deliveryManagerId));
    }

    private BooleanExpression relatedCompany(UUID companyId) {
        return companyId == null ? Expressions.FALSE
                : delivery.supplierCompanyIds.contains(companyId).or(delivery.recipientCompanyId.eq(companyId));
    }

    private OrderSpecifier<?> resolveOrder(Pageable pageable) {
        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(Sort.Order.desc("createdAt"));
        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

        return switch (sortOrder.getProperty()) {
            case "createdAt" -> new OrderSpecifier<>(direction, deliveryRoute.createdAt);
            case "updatedAt" -> new OrderSpecifier<>(direction, deliveryRoute.updatedAt);
            case "sequence" -> new OrderSpecifier<>(direction, deliveryRoute.routeSequence);
            case "startedAt" -> new OrderSpecifier<>(direction, deliveryRoute.startedAt);
            case "completedAt" -> new OrderSpecifier<>(direction, deliveryRoute.completedAt);
            default -> throw new BaseException(CommonErrorCode.INVALID_PARAMETER);
        };
    }
}
