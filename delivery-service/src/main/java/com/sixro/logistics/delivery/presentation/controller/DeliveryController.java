package com.sixro.logistics.delivery.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.delivery.application.service.DeliveryService;
import com.sixro.logistics.delivery.application.command.GetDeliveryCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveriesCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryStatusCommand;
import com.sixro.logistics.delivery.application.result.DeliverySearchResult;
import com.sixro.logistics.delivery.application.result.DeliveryResult;
import com.sixro.logistics.delivery.application.result.DeliveryStatusResult;
import com.sixro.logistics.delivery.presentation.dto.req.DeliverySearchReqDto;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryStatusUpdateReqDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryInfoResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliverySearchResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryStatusUpdateResDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{deliveryId}")
    public CommonResponse<DeliveryInfoResDto> getDelivery(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryId) {

        GetDeliveryCommand command = new GetDeliveryCommand(deliveryId, loginUserId, userRole, affiliationId);

        DeliveryResult result = deliveryService.getDelivery(command);

        return CommonResponse.success("배송을 조회했습니다.", new DeliveryInfoResDto(result));
    }

    @PatchMapping("/{deliveryId}/status")
    public CommonResponse<DeliveryStatusUpdateResDto> updateDeliveryStatus(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryStatusUpdateReqDto requestDto) {

        UpdateDeliveryStatusCommand command = new UpdateDeliveryStatusCommand(
                deliveryId, requestDto.getDeliveryStatus(), loginUserId, userRole, affiliationId);

        DeliveryStatusResult result = deliveryService.updateDeliveryStatus(command);

        return CommonResponse.success("배송 상태가 변경되었습니다.", new DeliveryStatusUpdateResDto(result));
    }

    @GetMapping
    public CommonResponse<PageResponse<DeliverySearchResDto>> searchDeliveries(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @ModelAttribute DeliverySearchReqDto searchReqDto) {

        SearchDeliveriesCommand command = new SearchDeliveriesCommand(
                loginUserId, userRole, affiliationId, searchReqDto.getOrderId(), searchReqDto.getDeliveryStatus(),
                searchReqDto.getOriginHubId(), searchReqDto.getDestHubId(), searchReqDto.getDeliveryManagerId(),
                searchReqDto.getDeadline(), pageable);

        Page<DeliverySearchResult> resultPage = deliveryService.searchDeliveries(command);
        PageResponse<DeliverySearchResDto> response = PageResponse.from(resultPage, DeliverySearchResDto::new);

        return CommonResponse.success("배송 목록을 조회했습니다.", response);
    }
}
