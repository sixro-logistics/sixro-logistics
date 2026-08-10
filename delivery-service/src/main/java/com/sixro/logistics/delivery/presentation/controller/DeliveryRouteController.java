package com.sixro.logistics.delivery.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.delivery.application.command.GetDeliveryRouteCommand;
import com.sixro.logistics.delivery.application.command.SearchDeliveryRoutesCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryRouteManagerCommand;
import com.sixro.logistics.delivery.application.command.UpdateDeliveryRouteStatusCommand;
import com.sixro.logistics.delivery.application.result.DeliveryRouteManagerResult;
import com.sixro.logistics.delivery.application.result.DeliveryRouteResult;
import com.sixro.logistics.delivery.application.result.DeliveryRouteStatusResult;
import com.sixro.logistics.delivery.application.service.DeliveryRouteService;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryRouteSearchReqDto;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryRouteManagerUpdateReqDto;
import com.sixro.logistics.delivery.presentation.dto.req.DeliveryRouteStatusUpdateReqDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryRouteInfoResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryRouteManagerUpdateResDto;
import com.sixro.logistics.delivery.presentation.dto.res.DeliveryRouteStatusUpdateResDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @PatchMapping("/{deliveryRouteId}/status")
    public CommonResponse<DeliveryRouteStatusUpdateResDto> updateDeliveryRouteStatus(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryRouteId,
            @Valid @RequestBody DeliveryRouteStatusUpdateReqDto requestDto) {

        UpdateDeliveryRouteStatusCommand command = new UpdateDeliveryRouteStatusCommand(
                deliveryRouteId, requestDto.getRouteStatus(), loginUserId, userRole, affiliationId);

        DeliveryRouteStatusResult result = deliveryRouteService.updateDeliveryRouteStatus(command);

        return CommonResponse.success("배송 경로 상태가 변경되었습니다.", new DeliveryRouteStatusUpdateResDto(result));
    }

    @PatchMapping("/{deliveryRouteId}/manager")
    public CommonResponse<DeliveryRouteManagerUpdateResDto> updateDeliveryRouteManager(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryRouteId,
            @Valid @RequestBody DeliveryRouteManagerUpdateReqDto requestDto) {

        UpdateDeliveryRouteManagerCommand command = new UpdateDeliveryRouteManagerCommand(
                deliveryRouteId, requestDto.getDeliveryManagerId(), loginUserId, userRole, affiliationId);

        DeliveryRouteManagerResult result = deliveryRouteService.updateDeliveryRouteManager(command);

        return CommonResponse.success(
                "허브 배송 담당자가 배정 또는 변경되었습니다.", new DeliveryRouteManagerUpdateResDto(result));
    }

    @GetMapping
    public CommonResponse<PageResponse<DeliveryRouteInfoResDto>> searchDeliveryRoutes(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            @ModelAttribute DeliveryRouteSearchReqDto searchReqDto) {

        SearchDeliveryRoutesCommand command = new SearchDeliveryRoutesCommand(
                loginUserId, userRole, affiliationId, searchReqDto.getDeliveryId(), searchReqDto.getRouteStatus(),
                searchReqDto.getOriginHubId(), searchReqDto.getDestHubId(), searchReqDto.getDeliveryManagerId(),
                searchReqDto.getRouteSequence(), pageable);

        Page<DeliveryRouteResult> resultPage = deliveryRouteService.searchDeliveryRoutes(command);
        PageResponse<DeliveryRouteInfoResDto> response = PageResponse.from(resultPage, DeliveryRouteInfoResDto::new);

        return CommonResponse.success("배송 경로 목록을 조회했습니다.", response);
    }
}
