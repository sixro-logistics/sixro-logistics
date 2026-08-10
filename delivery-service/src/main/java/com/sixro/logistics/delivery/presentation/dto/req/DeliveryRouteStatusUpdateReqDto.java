package com.sixro.logistics.delivery.presentation.dto.req;

import com.sixro.logistics.delivery.domain.enums.RouteStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryRouteStatusUpdateReqDto {

    @NotNull
    private RouteStatus routeStatus;
}
