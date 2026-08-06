package com.sixro.logistics.delivery.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.delivery.application.DeliveryManagerService;
import com.sixro.logistics.delivery.domain.entity.DeliveryManager;
import com.sixro.logistics.delivery.presentation.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerController {
    public final DeliveryManagerService managerService;

    public DeliveryManagerController(DeliveryManagerService managerService) {
        this.managerService = managerService;
    }

    // 배송 담당자 등록
    @PostMapping
    public CommonResponse<ManagerCreateResDto> createDeliveryManager(
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @Valid @RequestBody ManagerCreateReqDto managerCreateReqDto) {

        return CommonResponse.created(
                "배송 담당자가 등록되었습니다.",
                managerService.createDeliveryManager(userRole, affiliationId, managerCreateReqDto)
                );
    }

    // 배송 담당자 단건 조회
    @GetMapping("/{deliveryManagerId}")
    public CommonResponse<ManagerInfoResDto> getDeliveryManager(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryManagerId) {
        return CommonResponse.success("배송 담당자가 조회되었습니다.",managerService.getDeliveryManager(loginUserId, userRole, affiliationId, deliveryManagerId));
    }

    // 배송 담당자 목록 조회
    @GetMapping
    public CommonResponse<PageResponse<ManagerSearchResDto>> searchAllDeliveryManagers(
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PageableDefault(page=0, size=10, sort="createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @ModelAttribute ManagerSearchReqDto searchReqDto) {

        Page<DeliveryManager> resultPage = managerService.searchAllDeliveryManagers(userRole, affiliationId, searchReqDto, pageable);
        PageResponse<ManagerSearchResDto> res = PageResponse.from(resultPage, ManagerSearchResDto::new);

        return CommonResponse.success("배송 담당자 목록이 조회되었습니다.", res);

    }

    // 배송 담당자 정보 수정
    @PatchMapping("/{deliveryManagerId}")
    public CommonResponse<ManagerUpdateResDto> updateDeliveryManager(
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryManagerId,
            @Valid @RequestBody ManagerUpdateReqDto managerUpdateReqDto) {
        return CommonResponse.success("배송 담당자 정보가 수정되었습니다.", managerService.updateDeliveryManager(userRole, affiliationId, deliveryManagerId, managerUpdateReqDto));
    }

    // 배송 담당자 삭제
    @DeleteMapping("/{deliveryManagerId}")
    public ResponseEntity<Void> deleteDeliveryManager(
            @RequestHeader(HeaderConstants.USER_ID) UUID loginUserId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID deliveryManagerId) {
        managerService.deleteDeliveryManager(loginUserId, userRole, affiliationId, deliveryManagerId);

        return ResponseEntity.noContent().build();
    }
}
