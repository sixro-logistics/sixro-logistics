package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryStatusResult;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryStatusUpdateResDto {

    private final UUID deliveryId;
    private final UUID orderId;
    private final DeliveryStatus previousStatus;
    private final DeliveryStatus deliveryStatus;
    private final LocalDateTime changedAt;
    private final UUID changedBy;

    public DeliveryStatusUpdateResDto(DeliveryStatusResult result) {
        this.deliveryId = result.getDeliveryId();
        this.orderId = result.getOrderId();
        this.previousStatus = result.getPreviousStatus();
        this.deliveryStatus = result.getDeliveryStatus();
        this.changedAt = result.getChangedAt();
        this.changedBy = result.getChangedBy();
    }
}
