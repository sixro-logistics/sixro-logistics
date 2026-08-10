package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.DeliveryRoute;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryRouteManagerResult {

    private final UUID deliveryRouteId;
    private final UUID deliveryId;
    private final UUID previousDeliveryManagerId;
    private final UUID deliveryManagerId;
    private final ManagerType managerType;
    private final LocalDateTime updatedAt;
    private final UUID updatedBy;

    public DeliveryRouteManagerResult(DeliveryRoute deliveryRoute, UUID previousDeliveryManagerId) {
        this.deliveryRouteId = deliveryRoute.getDeliveryRouteId();
        this.deliveryId = deliveryRoute.getDelivery().getDeliveryId();
        this.previousDeliveryManagerId = previousDeliveryManagerId;
        this.deliveryManagerId = deliveryRoute.getDeliveryManager().getDeliveryManagerId();
        this.managerType = deliveryRoute.getDeliveryManager().getManagerType();
        this.updatedAt = deliveryRoute.getUpdatedAt();
        this.updatedBy = deliveryRoute.getUpdatedBy();
    }
}
