package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    boolean existsByDeliveryManager_DeliveryManagerIdAndRouteStatusNotIn(UUID deliveryManagerId, List<RouteStatus> completedStatuses);

    boolean existsByDelivery_DeliveryIdAndDeliveryManager_DeliveryManagerId(UUID deliveryId, UUID deliveryManagerId);

    Optional<DeliveryRoute> findByDelivery_DeliveryIdAndRouteSequence(UUID deliveryId, Integer routeSequence);

    boolean existsByDelivery_DeliveryIdAndRouteSequenceGreaterThan(UUID deliveryId, Integer routeSequence);
}
