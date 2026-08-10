package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliverySearchResult;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliverySearchResDto {

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

    public DeliverySearchResDto(DeliverySearchResult result) {
        this.deliveryId = result.getDeliveryId();
        this.orderId = result.getOrderId();
        this.deliveryStatus = result.getDeliveryStatus();
        this.originHubId = result.getOriginHubId();
        this.destHubId = result.getDestHubId();
        this.deliveryManagerId = result.getDeliveryManagerId();
        this.deliveryAddress = result.getDeliveryAddress();
        this.deliveryDeadline = result.getDeliveryDeadline();
        this.recipientName = result.getRecipientName();
        this.createdAt = result.getCreatedAt();
        this.updatedAt = result.getUpdatedAt();
    }
}
