package com.sixro.logistics.delivery.infrastructure.repository;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    @Query(value = "select exists (select 1 from delivery_schema.p_delivery where order_id = :orderId)", nativeQuery = true)
    boolean existsByOrderIdIncludingDeleted(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Delivery> findByDeliveryId(UUID deliveryId);

    boolean existsByDeliveryManager_DeliveryManagerIdAndDeliveryStatusNotIn(UUID deliveryManagerId, List<DeliveryStatus> completedStatuses);
}
