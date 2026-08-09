package com.sixro.logistics.delivery.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.delivery.application.service.DeliveryService;
import com.sixro.logistics.delivery.application.command.GetDeliveryCommand;
import com.sixro.logistics.delivery.application.command.DeleteDeliveryCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveriesCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryInfoCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryManagerCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryStatusCommand;
import com.sixro.logistics.delivery.application.result.DeliveryInfoUpdateResult;
import com.sixro.logistics.delivery.application.result.DeliveryDeleteResult;
import com.sixro.logistics.delivery.application.result.DeliveryManagerAssignmentResult;
import com.sixro.logistics.delivery.application.result.DeliverySearchResult;
import com.sixro.logistics.delivery.application.result.DeliveryResult;
import com.sixro.logistics.delivery.application.result.DeliveryStatusResult;
import com.sixro.logistics.delivery.presentation.dto.req.DeliverySearchReqDto;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryInfoUpdateReqDto;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryManagerUpdateReqDto;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryStatusUpdateReqDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryInfoResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryDeleteResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryInfoUpdateResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryManagerUpdateResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliverySearchResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryStatusUpdateResDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @PatchMapping("/{deliveryId}/manager")
    public CommonResponse<DeliveryManagerUpdateResDto> updateDeliveryManager(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryManagerUpdateReqDto requestDto) {

        UpdateDeliveryManagerCommand command = new UpdateDeliveryManagerCommand(
                deliveryId, requestDto.getDeliveryManagerId(), loginUserId, userRole, affiliationId);

        DeliveryManagerAssignmentResult result = deliveryService.updateDeliveryManager(command);

        return CommonResponse.success("업체 배송 담당자가 배정 또는 변경되었습니다.", new DeliveryManagerUpdateResDto(result));
    }

    @PatchMapping("/{deliveryId}")
    public CommonResponse<DeliveryInfoUpdateResDto> updateDeliveryInfo(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryInfoUpdateReqDto requestDto) {

        UpdateDeliveryInfoCommand command = new UpdateDeliveryInfoCommand(
                deliveryId, requestDto.getDeliveryAddress(), requestDto.getDeliveryDeadline(), requestDto.getRequests(),
                requestDto.getRecipientName(), requestDto.getRecipientSlackId(), loginUserId, userRole, affiliationId);

        DeliveryInfoUpdateResult result = deliveryService.updateDeliveryInfo(command);

        return CommonResponse.success("배송 정보가 수정되었습니다.", new DeliveryInfoUpdateResDto(result));
    }

    @DeleteMapping("/{deliveryId}")
    public CommonResponse<DeliveryDeleteResDto> deleteDelivery(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryId) {

        DeleteDeliveryCommand command = new DeleteDeliveryCommand(deliveryId, loginUserId, userRole, affiliationId);

        DeliveryDeleteResult result = deliveryService.deleteDelivery(command);

        return CommonResponse.success("배송이 삭제되었습니다.", new DeliveryDeleteResDto(result));
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
