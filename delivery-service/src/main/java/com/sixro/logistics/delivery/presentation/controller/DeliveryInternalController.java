package com.sixro.logistics.delivery.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.result.DeliveryManagerIdsResult;
import com.sixro.logistics.delivery.application.service.DeliveryInternalService;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryManagerIdsResDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "배송 내부 API", description = "서비스 간 배송 내부 API")
@RestController
@RequestMapping("/api/v1/internal/deliveries")
public class DeliveryInternalController {

    private final DeliveryInternalService deliveryInternalService;

    public DeliveryInternalController(DeliveryInternalService deliveryInternalService) {
        this.deliveryInternalService = deliveryInternalService;
    }

    @GetMapping
    public CommonResponse<DeliveryManagerIdsResDto> getDeliveryManagerIds(
            @RequestParam("orderId") UUID orderId) {

        DeliveryManagerIdsResult result = deliveryInternalService.getDeliveryManagerIds(orderId);
        DeliveryManagerIdsResDto response = new DeliveryManagerIdsResDto(result);

        return CommonResponse.success("배송 담당자 ID 목록이 조회되었습니다.", response);
    }
}
