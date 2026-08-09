package com.sixro.logistics.delivery.presentation.dto.req;

import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeliveryRouteSearchReqDto {

    private UUID deliveryId;
    private RouteStatus routeStatus;
    private UUID originHubId;
    private UUID destHubId;
    private UUID deliveryManagerId;
    private Integer routeSequence;
}
