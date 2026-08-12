package com.sixro.logistics.delivery.presentation.dto.res;

import com.sixro.logistics.delivery.application.result.DeliveryManagerIdsResult;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class DeliveryManagerIdsResDto {

    private final List<UUID> deliveryManagerIds;

    public DeliveryManagerIdsResDto(DeliveryManagerIdsResult result) {
        this.deliveryManagerIds = result.getDeliveryManagerIds();
    }
}
