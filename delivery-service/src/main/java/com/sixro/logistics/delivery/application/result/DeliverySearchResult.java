package com.sixro.logistics.delivery.application.result;

import com.sixro.logistics.delivery.domain.entity.Delivery;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliverySearchResult {

    private final UUID deliveryId;
    private final UUID orderId;
    private final DeliveryStatus deliveryStatus;
    private final UUID originHubId;
    private final UUID destHubId;
    private final UUID deliveryManagerId;
    private final String deliveryAddress;
    private final LocalDateTime deliveryDeadline;
    private final String recipientName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public DeliverySearchResult(Delivery delivery) {
        this.deliveryId = delivery.getDeliveryId();
        this.orderId = delivery.getOrderId();
        this.deliveryStatus = delivery.getDeliveryStatus();
        this.originHubId = delivery.getOriginHubId();
        this.destHubId = delivery.getDestHubId();
        this.deliveryManagerId = (delivery.getDeliveryManager() == null) ? null : delivery.getDeliveryManager().getDeliveryManagerId();
        this.deliveryAddress = delivery.getDeliveryAddress();
        this.deliveryDeadline = delivery.getDeliveryDeadline();
        this.recipientName = delivery.getRecipientName();
        this.createdAt = delivery.getCreatedAt();
        this.updatedAt = delivery.getUpdatedAt();
    }
}
