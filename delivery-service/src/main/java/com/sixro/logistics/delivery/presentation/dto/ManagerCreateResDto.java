package com.sixro.logistics.delivery.presentation.dto;

import com.sixro.logistics.delivery.domain.enums.ManagerStatus;
import com.sixro.logistics.delivery.domain.enums.ManagerType;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ManagerCreateResDto {
    private UUID deliveryManagerId;
    private ManagerType managerType;
    private UUID hubId;
    private Integer deliverySequence;
    private ManagerStatus managerStatus;
}
