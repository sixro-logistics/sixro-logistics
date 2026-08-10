package com.sixro.logistics.delivery.presentation.dto.req;

import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class DeliverySearchReqDto {

    private UUID orderId;
    private DeliveryStatus deliveryStatus;
    private UUID originHubId;
    private UUID destHubId;
    private UUID deliveryManagerId;
    private LocalDateTime deadline;
}
