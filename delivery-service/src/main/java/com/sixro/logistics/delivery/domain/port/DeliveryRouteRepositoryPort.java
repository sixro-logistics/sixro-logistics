package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepositoryPort {

    Optional<DeliveryRoute> findById(UUID deliveryRouteId);

    Page<DeliveryRoute> searchDeliveryRoutes(DeliveryRouteSearchCondition condition, Pageable pageable);

    boolean existsAssignedDeliveryManager(UUID deliveryId, UUID deliveryManagerId);
}
