package com.sixro.logistics.delivery.domain.port;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.DeliverySearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepositoryPort {

    Optional<Delivery> findById(UUID deliveryId);

    Page<Delivery> searchDeliveries(DeliverySearchCondition condition, Pageable pageable);

    void flush();
}
