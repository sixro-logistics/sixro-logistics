package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.DeliveryRouteSearchCondition;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteRepositoryPort {

    Optional<DeliveryRoute> findById(UUID deliveryRouteId);

    Optional<DeliveryRoute> findByDeliveryIdAndRouteSequence(UUID deliveryId, Integer routeSequence);

    List<DeliveryRoute> findAllWaitingByDeliveryId(UUID deliveryId);

    Page<DeliveryRoute> searchDeliveryRoutes(DeliveryRouteSearchCondition condition, Pageable pageable);

    boolean existsAssignedDeliveryManager(UUID deliveryId, UUID deliveryManagerId);

    boolean existsByDeliveryIdAndRouteSequenceGreaterThan(UUID deliveryId, Integer routeSequence);

    void flush();
}
