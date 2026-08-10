package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryResult;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryInfoResDto {

    private final UUID deliveryId;
    private final UUID orderId;
    private final UUID originHubId;
    private final UUID destHubId;
    private final UUID deliveryManagerId;
    private final DeliveryStatus deliveryStatus;
    private final String deliveryAddress;
    private final LocalDateTime deliveryDeadline;
    private final String requests;
    private final String recipientName;
    private final String recipientSlackId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public DeliveryInfoResDto(DeliveryResult result) {
        this.deliveryId = result.getDeliveryId();
        this.orderId = result.getOrderId();
        this.originHubId = result.getOriginHubId();
        this.destHubId = result.getDestHubId();
        this.deliveryManagerId = result.getDeliveryManagerId();
        this.deliveryStatus = result.getDeliveryStatus();
        this.deliveryAddress = result.getDeliveryAddress();
        this.deliveryDeadline = result.getDeliveryDeadline();
        this.requests = result.getRequests();
        this.recipientName = result.getRecipientName();
        this.recipientSlackId = result.getRecipientSlackId();
        this.createdAt = result.getCreatedAt();
        this.updatedAt = result.getUpdatedAt();
    }
}
