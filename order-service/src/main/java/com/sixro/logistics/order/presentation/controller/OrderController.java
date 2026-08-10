package com.sixro.logistics.order.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.order.application.facade.order.OrderFacade;
import com.sixro.logistics.order.application.result.OrderCreateResult;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.presentation.dto.request.OrderCreateRequestDto;
import com.sixro.logistics.order.presentation.dto.response.OrderCreateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<OrderCreateResponseDto>> createOrder(
            /*@RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole userRole,*/
            @Valid @RequestBody OrderCreateRequestDto requestDto){

        // TO DO: Gateway에서 전달받은 인증 헤더로 대체
        OrderCreateResult createResult
                = orderFacade.createOrder(UUID.randomUUID(), UserRole.HUB_ADMIN, requestDto.toCommand());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED,
                        "주문이 생성되었습니다.",
                        OrderCreateResponseDto.from(createResult)
                ));
    }

}
