package com.sixro.logistics.delivery.presentation;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.DeliveryManagerService;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateReqDto;
import com.sixro.logistics.delivery.presentation.dto.ManagerCreateResDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DeliveryManagerController {
    public final DeliveryManagerService managerService;

    public DeliveryManagerController(DeliveryManagerService managerService) {
        this.managerService = managerService;
    }

    // 배송 담당자 등록
    @PostMapping("/delivery-managers")
    public CommonResponse<ManagerCreateResDto> createDeliveryManager(
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @Valid @RequestBody ManagerCreateReqDto managerCreateReqDto) {

        return CommonResponse.created(
                "배송 담당자가 등록되었습니다.",
                managerService.createDeliveryManager(userRole, affiliationId, managerCreateReqDto)
                );
    }
}
