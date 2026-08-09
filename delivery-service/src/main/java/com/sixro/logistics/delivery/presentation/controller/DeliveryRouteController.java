package com.sixro.logistics.delivery.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.DeliveryRouteService;
import com.sixro.logistics.delivery.application.command.GetDeliveryRouteCommand;
import com.sixro.logistics.delivery.application.result.DeliveryRouteResult;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryRouteInfoResDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-routes")
public class DeliveryRouteController {

    private final DeliveryRouteService deliveryRouteService;

    public DeliveryRouteController(DeliveryRouteService deliveryRouteService) {
        this.deliveryRouteService = deliveryRouteService;
    }

    @GetMapping("/{deliveryRouteId}")
    public CommonResponse<DeliveryRouteInfoResDto> getDeliveryRoute(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryRouteId) {

        GetDeliveryRouteCommand command = new GetDeliveryRouteCommand(deliveryRouteId, loginUserId, userRole, affiliationId);

        DeliveryRouteResult result = deliveryRouteService.getDeliveryRoute(command);

        return CommonResponse.success("배송 경로를 조회했습니다.", new DeliveryRouteInfoResDto(result));
    }
}
