package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ManagerSearchResDto {
    private UUID deliveryManagerId;
    private ManagerType managerType;
    private UUID hubId;
    private Integer deliverySequence;
    private ManagerStatus managerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ManagerSearchResDto(DeliveryManager deliveryManager) {
        this.deliveryManagerId = deliveryManager.getDeliveryManagerId();
        this.managerType = deliveryManager.getManagerType();
        this.hubId = deliveryManager.getHubId();
        this.deliverySequence = deliveryManager.getDeliverySequence();
        this.managerStatus = deliveryManager.getManagerStatus();
        this.createdAt = deliveryManager.getCreatedAt();
        this.updatedAt = deliveryManager.getUpdatedAt();
    }
}
