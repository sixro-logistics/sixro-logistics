package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryDeleteResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class DeliveryDeleteResDto {

    private final UUID deliveryId;
    private final int deletedRouteCount;
    private final LocalDateTime deletedAt;
    private final UUID deletedBy;

    public DeliveryDeleteResDto(DeliveryDeleteResult result) {
        this.deliveryId = result.getDeliveryId();
        this.deletedRouteCount = result.getDeletedRouteCount();
        this.deletedAt = result.getDeletedAt();
        this.deletedBy = result.getDeletedBy();
    }
}
