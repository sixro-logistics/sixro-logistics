package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryRouteQueryRepository {

    Page<DeliveryRoute> search(DeliveryRouteSearchCondition condition, Pageable pageable);
}
