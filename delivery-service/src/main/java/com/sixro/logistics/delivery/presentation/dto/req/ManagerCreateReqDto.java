package com.sixro.logistics.delivery.presentation.dto.req;

import com.sixro.logistics.delivery.domain.enums.ManagerType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ManagerCreateReqDto {
    @NotNull
    private UUID userId;

    @NotNull
    private ManagerType managerType;

    private UUID hubId;
}
