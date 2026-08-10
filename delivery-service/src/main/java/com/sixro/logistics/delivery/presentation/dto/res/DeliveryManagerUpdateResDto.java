package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryManagerAssignmentResult;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryManagerUpdateResDto {

    private final UUID deliveryId;
    private final UUID previousDeliveryManagerId;
    private final UUID deliveryManagerId;
    private final ManagerType managerType;
    private final LocalDateTime updatedAt;
    private final UUID updatedBy;

    public DeliveryManagerUpdateResDto(DeliveryManagerAssignmentResult result) {
        this.deliveryId = result.getDeliveryId();
        this.previousDeliveryManagerId = result.getPreviousDeliveryManagerId();
        this.deliveryManagerId = result.getDeliveryManagerId();
        this.managerType = result.getManagerType();
        this.updatedAt = result.getUpdatedAt();
        this.updatedBy = result.getUpdatedBy();
    }
}
