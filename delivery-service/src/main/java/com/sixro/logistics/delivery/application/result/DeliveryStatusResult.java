package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryStatusResult {

    private final UUID deliveryId;
    private final UUID orderId;
    private final DeliveryStatus previousStatus;
    private final DeliveryStatus deliveryStatus;
    private final LocalDateTime changedAt;
    private final UUID changedBy;

    public DeliveryStatusResult(Delivery delivery, DeliveryStatus previousStatus, LocalDateTime changedAt, UUID changedBy) {
        this.deliveryId = delivery.getDeliveryId();
        this.orderId = delivery.getOrderId();
        this.previousStatus = previousStatus;
        this.deliveryStatus = delivery.getDeliveryStatus();
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }
}
