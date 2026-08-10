package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryManagerAssignmentResult {

    private final UUID deliveryId;
    private final UUID previousDeliveryManagerId;
    private final UUID deliveryManagerId;
    private final ManagerType managerType;
    private final LocalDateTime updatedAt;
    private final UUID updatedBy;

    public DeliveryManagerAssignmentResult(Delivery delivery, UUID previousDeliveryManagerId) {
        this.deliveryId = delivery.getDeliveryId();
        this.previousDeliveryManagerId = previousDeliveryManagerId;
        this.deliveryManagerId = delivery.getDeliveryManager().getDeliveryManagerId();
        this.managerType = delivery.getDeliveryManager().getManagerType();
        this.updatedAt = delivery.getUpdatedAt();
        this.updatedBy = delivery.getUpdatedBy();
    }
}
