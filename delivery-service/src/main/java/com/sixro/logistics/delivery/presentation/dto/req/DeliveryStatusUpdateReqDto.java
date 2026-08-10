package com.sixro.logistics.delivery.presentation.dto.req;

import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryStatusUpdateReqDto {

    @NotNull
    private DeliveryStatus deliveryStatus;
}
