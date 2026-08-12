package com.sixro.logistics.delivery.infrastructure.repository.adapter;

import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import com.sixro.logistics.delivery.domain.port.DeliveryRouteRepositoryPort;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRouteQueryRepository;
import com.sixro.logistics.delivery.infrastructure.repository.DeliveryRouteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeliveryRouteRepositoryAdapter implements DeliveryRouteRepositoryPort {

    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryRouteQueryRepository deliveryRouteQueryRepository;

    public DeliveryRouteRepositoryAdapter(DeliveryRouteRepository deliveryRouteRepository,
                                          DeliveryRouteQueryRepository deliveryRouteQueryRepository) {
        this.deliveryRouteRepository = deliveryRouteRepository;
        this.deliveryRouteQueryRepository = deliveryRouteQueryRepository;
    }

    @Override
    public List<DeliveryRoute> saveAll(List<DeliveryRoute> deliveryRoutes) {
        return deliveryRouteRepository.saveAll(deliveryRoutes);
    }

    @Override
    public Optional<DeliveryRoute> findById(UUID deliveryRouteId) {
        return deliveryRouteRepository.findById(deliveryRouteId);
    }

    @Override
    public Optional<UUID> findDeliveryIdById(UUID deliveryRouteId) {
        return deliveryRouteRepository.findDeliveryIdById(deliveryRouteId);
    }

    @Override
    public Optional<DeliveryRoute> findByIdForUpdate(UUID deliveryRouteId) {
        return deliveryRouteRepository.findByDeliveryRouteId(deliveryRouteId);
    }

    @Override
    public Optional<DeliveryRoute> findByDeliveryIdAndRouteSequence(UUID deliveryId, Integer routeSequence) {
        return deliveryRouteRepository.findByDelivery_DeliveryIdAndRouteSequence(deliveryId, routeSequence);
    }

    @Override
    public List<DeliveryRoute> findAllWaitingByDeliveryId(UUID deliveryId) {
        return deliveryRouteRepository.findAllByDelivery_DeliveryIdAndRouteStatus(
                deliveryId, RouteStatus.HUB_TRANSIT_WAITING);
    }

    @Override
    public List<DeliveryRoute> findAllByDeliveryIdForUpdate(UUID deliveryId) {
        return deliveryRouteRepository.findAllByDelivery_DeliveryId(deliveryId);
    }

    @Override
    public List<DeliveryRoute> findAllByDeliveryId(UUID deliveryId) {
        return deliveryRouteRepository.findAllByDeliveryId(deliveryId);
    }

    @Override
    public List<DeliveryRoute> findAllByDeliveryIdOrderByRouteSequenceAsc(UUID deliveryId) {
        return deliveryRouteRepository.findAllByDelivery_DeliveryIdOrderByRouteSequenceAsc(deliveryId);
    }

    @Override
    public Page<DeliveryRoute> searchDeliveryRoutes(DeliveryRouteSearchCondition condition, Pageable pageable) {
        return deliveryRouteQueryRepository.search(condition, pageable);
    }

    @Override
    public boolean existsAssignedDeliveryManager(UUID deliveryId, UUID deliveryManagerId) {
        return deliveryRouteRepository
                .existsByDelivery_DeliveryIdAndDeliveryManager_DeliveryManagerId(deliveryId, deliveryManagerId);
    }

    @Override
    public boolean existsByDeliveryIdAndRouteSequenceGreaterThan(UUID deliveryId, Integer routeSequence) {
        return deliveryRouteRepository
                .existsByDelivery_DeliveryIdAndRouteSequenceGreaterThan(deliveryId, routeSequence);
    }

    @Override
    public void flush() {
        deliveryRouteRepository.flush();
    }
}
