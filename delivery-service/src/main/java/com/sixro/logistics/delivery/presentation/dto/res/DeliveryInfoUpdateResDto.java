package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryInfoUpdateResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryInfoUpdateResDto {

    private final UUID deliveryId;
    private final String deliveryAddress;
    private final LocalDateTime deliveryDeadline;
    private final String requests;
    private final String recipientName;
    private final String recipientSlackId;
    private final LocalDateTime updatedAt;
    private final UUID updatedBy;

    public DeliveryInfoUpdateResDto(DeliveryInfoUpdateResult result) {
        this.deliveryId = result.getDeliveryId();
        this.deliveryAddress = result.getDeliveryAddress();
        this.deliveryDeadline = result.getDeliveryDeadline();
        this.requests = result.getRequests();
        this.recipientName = result.getRecipientName();
        this.recipientSlackId = result.getRecipientSlackId();
        this.updatedAt = result.getUpdatedAt();
        this.updatedBy = result.getUpdatedBy();
    }
}
