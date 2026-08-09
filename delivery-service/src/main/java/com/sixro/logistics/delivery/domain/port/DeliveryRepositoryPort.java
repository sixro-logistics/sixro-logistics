package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.entity.Delivery;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepositoryPort {

    Optional<Delivery> findById(UUID deliveryId);
}
