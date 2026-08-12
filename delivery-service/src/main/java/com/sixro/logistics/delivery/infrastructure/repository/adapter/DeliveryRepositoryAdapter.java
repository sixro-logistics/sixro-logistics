package com.sixro.logistics.delivery.infrastructure.repository.adapter;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.port.DeliveryRepositoryPort;
import com.sixro.logistics.delivery.domain.DeliverySearchCondition;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryQueryRepository;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DeliveryRepositoryAdapter implements DeliveryRepositoryPort {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryQueryRepository deliveryQueryRepository;

    public DeliveryRepositoryAdapter(DeliveryRepository deliveryRepository, DeliveryQueryRepository deliveryQueryRepository) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryQueryRepository = deliveryQueryRepository;
    }

    @Override
    public boolean existsByOrderIdIncludingDeleted(UUID orderId) {
        return deliveryRepository.existsByOrderIdIncludingDeleted(orderId);
    }

    @Override
    public Delivery save(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    @Override
    public Optional<Delivery> findById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId);
    }

    @Override
    public Optional<Delivery> findByOrderId(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Delivery> findByIdForUpdate(UUID deliveryId) {
        return deliveryRepository.findByDeliveryId(deliveryId);
    }

    @Override
    public Page<Delivery> searchDeliveries(DeliverySearchCondition condition, Pageable pageable) {
        return deliveryQueryRepository.search(condition, pageable);
    }

    @Override
    public void flush() {
        deliveryRepository.flush();
    }
}
