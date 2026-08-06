package com.sixro.logistics.delivery.infrastructure;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {
    boolean existsByDeliveryManager_DeliveryManagerIdAndRouteStatusNotIn(UUID deliveryManagerId, List<RouteStatus> completedStatuses);
}
