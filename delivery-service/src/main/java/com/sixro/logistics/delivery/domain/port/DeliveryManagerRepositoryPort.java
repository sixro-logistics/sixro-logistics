package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepositoryPort {

    Optional<DeliveryManager> findByIdForUpdate(UUID deliveryManagerId);
}
