package com.sixro.logistics.hub.route.infrastructure.persistence.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sixro.logistics.hub.route.domain.model.HubRoute;
import com.sixro.logistics.hub.route.domain.repository.HubRouteQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sixro.logistics.hub.route.domain.model.QHubRoute.hubRoute;

@Repository
@RequiredArgsConstructor
public class HubRouteQueryAdapter implements HubRouteQueryRepository {

    private static final String HUB_NETWORK_ACTIVE = "hub:network:active";

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<HubRoute> findById(UUID id) {
        HubRoute result = queryFactory
                .selectFrom(hubRoute)
                .where(
                        eqId(id),
                        isNotDeleted()
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<HubRoute> search(UUID originHubId, UUID destinationHubId, Pageable pageable) {

        JPAQuery<HubRoute> query = queryFactory
                .selectFrom(hubRoute)
                .where(
                        isNotDeleted(),
                        eqOriginHubId(originHubId),
                        eqDestinationHubId(destinationHubId)
                )
                .orderBy(hubRoute.createdAt.desc());

        // Pageable이 unpaged가 아닐 때만 offset/limit 적용
        if (pageable.isPaged()) {
            query
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize());
        }

        List<HubRoute> content = query.fetch();

        Long total = queryFactory.select(hubRoute.count())
                .from(hubRoute)
                .where(
                        isNotDeleted(),
                        eqOriginHubId(originHubId),
                        eqDestinationHubId(destinationHubId)
                )
                .fetchOne();

        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    /**
     * 다익스트라 연산용 활성 노선망 전체 조회 캐싱
     */
    @Override
    @Cacheable(cacheNames = HUB_NETWORK_ACTIVE)
    public List<HubRoute> findAllActiveRoutes() {
        return queryFactory
                .selectFrom(hubRoute)
                .where(isNotDeleted())
                .fetch();
    }

    private BooleanExpression isNotDeleted() {
        return hubRoute.isDeleted.isFalse();
    }

    private BooleanExpression eqId(UUID id) {
        return id != null ? hubRoute.id.eq(id) : null;
    }

    private BooleanExpression eqOriginHubId(UUID originHubId) {
        return originHubId != null ? hubRoute.originHubId.eq(originHubId) : null;
    }

    private BooleanExpression eqDestinationHubId(UUID destinationHubId) {
        return destinationHubId != null ? hubRoute.destinationHubId.eq(destinationHubId) : null;
    }
}