package com.sixro.logistics.delivery.domain;

import com.sixro.logistics.delivery.domain.enums.DeliverySearchScope;
import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliverySearchCondition(
        UUID orderId,
        DeliveryStatus deliveryStatus,
        UUID originHubId,
        UUID destHubId,
        UUID deliveryManagerId,
        LocalDateTime deadline,
        DeliverySearchScope searchScope,
        UUID scopeId) {

}
