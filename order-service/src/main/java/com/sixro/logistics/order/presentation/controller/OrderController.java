package com.sixro.logistics.order.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.order.application.facade.order.OrderCommandFacade;
import com.sixro.logistics.order.application.facade.order.OrderQueryFacade;
import com.sixro.logistics.order.application.result.*;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.sixro.logistics.order.presentation.dto.request.OrderSearchRequestDto;
import com.sixro.logistics.order.presentation.dto.request.OrderUpdateRequestDto;
import com.sixro.logistics.order.presentation.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandFacade orderCommandFacade;
    private final OrderQueryFacade orderQueryFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<OrderCreateResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto requestDto){

        OrderCreateResult result
                = orderCommandFacade.createOrder(requestDto.toCommand());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED,
                        "주문이 생성되었습니다.",
                        OrderCreateResponseDto.from(result)
                ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<CommonResponse<OrderGetOneResponseDto>> getOneOrder(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID orderId){

        OrderGetOneResult result =
                orderQueryFacade.getOneOrder(userRole, affiliationId, orderId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "주문 조회에 성공했습니다.",
                        OrderGetOneResponseDto.from(result)
                ));
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<CommonResponse<OrderUpdateResponseDto>> updateOrder(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderUpdateRequestDto requestDto
    ){

        OrderUpdateResult result =
                orderCommandFacade.updateOrder(
                        userRole, affiliationId, orderId, requestDto.toCommand()
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "주문이 수정되었습니다.",
                        OrderUpdateResponseDto.from(result)
                ));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<CommonResponse<OrderDeleteResponseDto>> deleteOrder(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID orderId
            ){

        OrderDeleteResult result =
                orderCommandFacade.deleteOrder(userId, userRole, affiliationId, orderId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "주문이 삭제되었습니다.",
                        OrderDeleteResponseDto.from(result)
                ));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<CommonResponse<OrderCancelResponseDto>> cancelOrder(
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @PathVariable UUID orderId){

        OrderCancelResult result =
                orderCommandFacade.cancelOrder(userRole, affiliationId, orderId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "주문이 취소되었습니다.",
                        OrderCancelResponseDto.from(result)
                ));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<OrderSearchResponseDto>>> searchOrder(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(value = HeaderConstants.AFFILIATION_ID, required = false) UUID affiliationId,
            @ModelAttribute OrderSearchRequestDto requestDto,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC,
                    size = 10
            ) Pageable pageable
    ) {

        OrderSearchResult result = orderQueryFacade.searchOrder(
                userId, userRole, affiliationId, requestDto.toCommand(), pageable
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "주문 목록 조회에 성공했습니다.",
                        PageResponse.from(
                                result.page(),
                                item -> new OrderSearchResponseDto(
                                        item.orderId(),
                                        item.receiverId(),
                                        item.hubId(),
                                        item.receiverCompanyId(),
                                        item.deliveryAddress(),
                                        item.deliveryDeadline(),
                                        item.requests(),
                                        item.orderStatus()
                                )
                        )
                ));
    }

}
