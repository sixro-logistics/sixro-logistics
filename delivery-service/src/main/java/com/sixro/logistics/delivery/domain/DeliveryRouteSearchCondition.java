package com.sixro.logistics.delivery.domain;

import com.sixro.logistics.delivery.domain.enums.DeliverySearchScope;
import com.sixro.logistics.delivery.domain.enums.RouteStatus;

import java.util.UUID;

public record DeliveryRouteSearchCondition(
        UUID deliveryId,
        RouteStatus routeStatus,
        UUID originHubId,
        UUID destHubId,
        UUID deliveryManagerId,
        Integer routeSequence,
        DeliverySearchScope searchScope,
        UUID scopeId) {

}
