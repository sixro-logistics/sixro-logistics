package com.sixro.logistics.delivery.domain.port;

import java.util.UUID;

public interface DeliveryRouteRepositoryPort {

    boolean existsAssignedDeliveryManager(UUID deliveryId, UUID deliveryManagerId);
}
