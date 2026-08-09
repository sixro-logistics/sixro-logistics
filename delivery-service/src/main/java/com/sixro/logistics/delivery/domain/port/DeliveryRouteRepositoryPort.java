package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepositoryPort {

    Optional<DeliveryRoute> findById(UUID deliveryRouteId);

    boolean existsAssignedDeliveryManager(UUID deliveryId, UUID deliveryManagerId);
}
