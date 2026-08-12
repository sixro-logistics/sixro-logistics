package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    @Query("select deliveryRoute.delivery.deliveryId from DeliveryRoute deliveryRoute "
            + "where deliveryRoute.deliveryRouteId = :deliveryRouteId")
    Optional<UUID> findDeliveryIdById(@Param("deliveryRouteId") UUID deliveryRouteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryRoute> findByDeliveryRouteId(UUID deliveryRouteId);

    boolean existsByDeliveryManager_DeliveryManagerIdAndRouteStatusNotIn(UUID deliveryManagerId, List<RouteStatus> completedStatuses);

    boolean existsByDelivery_DeliveryIdAndDeliveryManager_DeliveryManagerId(UUID deliveryId, UUID deliveryManagerId);

    Optional<DeliveryRoute> findByDelivery_DeliveryIdAndRouteSequence(UUID deliveryId, Integer routeSequence);

    List<DeliveryRoute> findAllByDelivery_DeliveryIdAndRouteStatus(UUID deliveryId, RouteStatus routeStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DeliveryRoute> findAllByDelivery_DeliveryId(UUID deliveryId);

    @Query("select deliveryRoute from DeliveryRoute deliveryRoute "
            + "where deliveryRoute.delivery.deliveryId = :deliveryId")
    List<DeliveryRoute> findAllByDeliveryId(@Param("deliveryId") UUID deliveryId);

    List<DeliveryRoute> findAllByDelivery_DeliveryIdOrderByRouteSequenceAsc(UUID deliveryId);

    boolean existsByDelivery_DeliveryIdAndRouteSequenceGreaterThan(UUID deliveryId, Integer routeSequence);
}
