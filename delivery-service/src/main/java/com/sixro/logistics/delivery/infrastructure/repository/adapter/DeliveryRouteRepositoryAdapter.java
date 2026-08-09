package com.sixro.logistics.delivery.infrastructure.repository.adapter;

import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRouteRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class DeliveryRouteRepositoryAdapter implements DeliveryRouteRepositoryPort {

    private final DeliveryRouteRepository deliveryRouteRepository;

    public DeliveryRouteRepositoryAdapter(DeliveryRouteRepository deliveryRouteRepository) {
        this.deliveryRouteRepository = deliveryRouteRepository;
    }

    @Override
    public boolean existsAssignedDeliveryManager(UUID deliveryId, UUID deliveryManagerId) {
        return deliveryRouteRepository
                .existsByDelivery_DeliveryIdAndDeliveryManager_DeliveryManagerId(deliveryId, deliveryManagerId);
    }
}
