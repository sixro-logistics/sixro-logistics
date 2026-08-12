package com.sixro.logistics.order.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.order.application.facade.order.OrderCommandFacade;
import com.sixro.logistics.order.application.facade.order.OrderQueryFacade;
import com.sixro.logistics.order.application.result.*;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.sixro.logistics.order.presentation.dto.request.OrderSearchRequestDto;
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
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,*/
            @Valid @RequestBody OrderCreateRequestDto requestDto){

        // TO DO: Gateway에서 전달받은 인증 헤더로 대체
        OrderCreateResult result
                = orderCommandFacade.createOrder(UUID.randomUUID(), UserRole.HUB_ADMIN, requestDto.toCommand());

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
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId*/
            UUID userId, UserRole userRole, UUID affiliationId,
            @PathVariable UUID orderId){

        // TO DO: Gateway에서 전달받은 인증 헤더로 대체

        OrderGetOneResult result =
                orderQueryFacade.getOneOrder(userId, userRole, affiliationId, orderId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CommonResponse.success(
                        HttpStatus.OK,
                        "주문 조회에 성공했습니다.",
                        OrderGetOneResponseDto.from(result)
                ));
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<CommonResponse<OrderDeleteResponseDto>> deleteOrder(
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId*/
            UUID userId, UserRole userRole, UUID affiliationId,
            @PathVariable UUID orderId
            ){

        // TO DO: Gateway에서 전달받은 인증 헤더로 대체

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
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId*/
            UUID userId, UserRole userRole, UUID affiliationId,
            @PathVariable UUID orderId){

        // TO DO: Gateway에서 전달받은 인증 헤더로 대체

        OrderCancelResult result =
                orderCommandFacade.cancelOrder(userId, userRole, affiliationId, orderId);

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
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,
            @RequestHeader(HeaderConstants.AFFILIATION_ID) UUID affiliationId*/
            UUID userId, UserRole userRole, UUID affiliationId,
            @ModelAttribute OrderSearchRequestDto requestDto,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC,
                    size = 10
            ) Pageable pageable
    ) {

        // TO DO: Gateway에서 전달받은 인증 헤더로 대체

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
