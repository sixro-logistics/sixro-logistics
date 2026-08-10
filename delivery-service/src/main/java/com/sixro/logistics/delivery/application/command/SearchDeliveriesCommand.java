package com.sixro.logistics.delivery.application.command;

import com.sixro.logistics.delivery.domain.enums.DeliveryStatus;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class SearchDeliveriesCommand {

    private final UUID loginUserId;
    private final String userRole;
    private final UUID affiliationId;
    private final UUID orderId;
    private final DeliveryStatus deliveryStatus;
    private final UUID originHubId;
    private final UUID destHubId;
    private final UUID deliveryManagerId;
    private final LocalDateTime deadline;
    private final Pageable pageable;

    public SearchDeliveriesCommand(UUID loginUserId, String userRole, UUID affiliationId, UUID orderId,
                                   DeliveryStatus deliveryStatus, UUID originHubId, UUID destHubId,
                                   UUID deliveryManagerId, LocalDateTime deadline, Pageable pageable) {
        this.loginUserId = loginUserId;
        this.userRole = userRole;
        this.affiliationId = affiliationId;
        this.orderId = orderId;
        this.deliveryStatus = deliveryStatus;
        this.originHubId = originHubId;
        this.destHubId = destHubId;
        this.deliveryManagerId = deliveryManagerId;
        this.deadline = deadline;
        this.pageable = pageable;
    }
}
