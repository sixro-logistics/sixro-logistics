package com.sixro.logistics.delivery.infrastructure.repository.adapter;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DeliveryRepositoryAdapter implements DeliveryRepositoryPort {

    private final DeliveryRepository deliveryRepository;

    public DeliveryRepositoryAdapter(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public Optional<Delivery> findById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId);
    }

}
