package com.sixro.logistics.user.infrastructure.persistence.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;
import com.sixro.logistics.user.domain.repository.UserSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.sixro.logistics.user.domain.entity.QUser.user;

/**
 * QueryDSL을 사용해 사용자 목록을 동적으로 검색합니다.
 */
@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<User> search(
            UserSearchCondition condition,
            Pageable pageable
    ) {
        List<User> content = queryFactory
                .selectFrom(user)
                .where(searchConditions(condition))
                .orderBy(resolveOrder(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(user.count())
                .from(user)
                .where(searchConditions(condition))
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    private BooleanExpression[] searchConditions(
            UserSearchCondition condition
    ) {
        return new BooleanExpression[]{
                user.isDeleted.isFalse(),
                usernameContains(condition.username()),
                roleEq(condition.role()),
                userStatusEq(condition.userStatus()),
                affiliationTypeEq(condition.affiliationType())
        };
    }

    private BooleanExpression usernameContains(String username) {
        return username == null || username.isBlank()
                ? null
                : user.username.containsIgnoreCase(username.trim());
    }

    private BooleanExpression roleEq(UserRole role) {
        return role == null ? null : user.role.eq(role);
    }

    private BooleanExpression userStatusEq(UserStatus userStatus) {
        return userStatus == null
                ? null
                : user.userStatus.eq(userStatus);
    }

    private BooleanExpression affiliationTypeEq(
            AffiliationType affiliationType
    ) {
        return affiliationType == null
                ? null
                : user.affiliationType.eq(affiliationType);
    }

    /**
     * API 명세에서 허용한 createdAt, updatedAt 정렬을 적용합니다.
     */
    private OrderSpecifier<?> resolveOrder(Pageable pageable) {
        Sort.Order sortOrder = pageable.getSort()
                .stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));

        Order direction = sortOrder.isAscending()
                ? Order.ASC
                : Order.DESC;

        return switch (sortOrder.getProperty()) {
            case "createdAt" ->
                    new OrderSpecifier<>(direction, user.createdAt);
            case "updatedAt" ->
                    new OrderSpecifier<>(direction, user.updatedAt);
            default ->
                    throw new BaseException(
                            CommonErrorCode.INVALID_PARAMETER
                    );
        };
    }
}