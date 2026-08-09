package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryInfoUpdateResult {

    private final UUID deliveryId;
    private final String deliveryAddress;
    private final LocalDateTime deliveryDeadline;
    private final String requests;
    private final String recipientName;
    private final String recipientSlackId;
    private final LocalDateTime updatedAt;
    private final UUID updatedBy;

    public DeliveryInfoUpdateResult(Delivery delivery) {
        this.deliveryId = delivery.getDeliveryId();
        this.deliveryAddress = delivery.getDeliveryAddress();
        this.deliveryDeadline = delivery.getDeliveryDeadline();
        this.requests = delivery.getRequests();
        this.recipientName = delivery.getRecipientName();
        this.recipientSlackId = delivery.getRecipientSlackId();
        this.updatedAt = delivery.getUpdatedAt();
        this.updatedBy = delivery.getUpdatedBy();
    }
}
