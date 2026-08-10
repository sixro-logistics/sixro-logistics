package com.sixro.logistics.delivery.presentation.dto.req;

import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ManagerSearchReqDto {
    private ManagerType managerType;
    private UUID hubId;
    private ManagerStatus managerStatus;
    private Integer deliverySequence;
}
