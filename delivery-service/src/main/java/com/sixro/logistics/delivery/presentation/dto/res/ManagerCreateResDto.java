package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ManagerCreateResDto {
    private UUID deliveryManagerId;
    private ManagerType managerType;
    private UUID hubId;
    private Integer deliverySequence;
    private ManagerStatus managerStatus;

    public ManagerCreateResDto(DeliveryManager savedManager) {
        deliveryManagerId = savedManager.getDeliveryManagerId();
        managerType = savedManager.getManagerType();
        hubId = savedManager.getHubId();
        deliverySequence = savedManager.getDeliverySequence();
        managerStatus = savedManager.getManagerStatus();
    }
}
