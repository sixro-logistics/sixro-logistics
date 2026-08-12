package com.sixro.logistics.delivery.application.result;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class DeliveryManagerIdsResult {

    private final List<UUID> deliveryManagerIds;

    public DeliveryManagerIdsResult(List<UUID> deliveryManagerIds) {
        this.deliveryManagerIds = deliveryManagerIds;
    }
}
