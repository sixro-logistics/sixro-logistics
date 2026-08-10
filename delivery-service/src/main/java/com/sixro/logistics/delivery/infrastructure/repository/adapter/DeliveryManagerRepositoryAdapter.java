package com.sixro.logistics.delivery.infrastructure.repository.adapter;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.port.DeliveryManagerRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryManagerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DeliveryManagerRepositoryAdapter implements DeliveryManagerRepositoryPort {

    private final DeliveryManagerRepository deliveryManagerRepository;

    public DeliveryManagerRepositoryAdapter(DeliveryManagerRepository deliveryManagerRepository) {
        this.deliveryManagerRepository = deliveryManagerRepository;
    }

    @Override
    public Optional<DeliveryManager> findByIdForUpdate(UUID deliveryManagerId) {
        return deliveryManagerRepository.findByDeliveryManagerId(deliveryManagerId);
    }
}
