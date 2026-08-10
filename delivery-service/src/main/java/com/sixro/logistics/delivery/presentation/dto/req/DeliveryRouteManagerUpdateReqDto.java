package com.sixro.logistics.delivery.presentation.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeliveryRouteManagerUpdateReqDto {

    @NotNull
    private UUID deliveryManagerId;
}
