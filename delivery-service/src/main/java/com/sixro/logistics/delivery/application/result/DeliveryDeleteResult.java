package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryDeleteResult {

    private final UUID deliveryId;
    private final int deletedRouteCount;
    private final LocalDateTime deletedAt;
    private final UUID deletedBy;

    public DeliveryDeleteResult(Delivery delivery, int deletedRouteCount) {
        this.deliveryId = delivery.getDeliveryId();
        this.deletedRouteCount = deletedRouteCount;
        this.deletedAt = delivery.getDeletedAt();
        this.deletedBy = delivery.getDeletedBy();
    }
}
