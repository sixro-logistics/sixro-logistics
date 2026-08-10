package com.sixro.logistics.user.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.application.dto.InternalDeliveryUserResult;
import com.sixro.logistics.user.application.dto.InternalUserAuthResult;
import com.sixro.logistics.user.application.dto.InternalUserStatusResult;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.application.service.UserCommandService;
import com.sixro.logistics.user.application.service.UserQueryService;
import com.sixro.logistics.user.presentation.request.internal.InternalCreateUserRequest;
import com.sixro.logistics.user.presentation.response.internal.InternalCreateUserResponse;
import com.sixro.logistics.user.presentation.response.internal.InternalDeliveryUserResponse;
import com.sixro.logistics.user.presentation.response.internal.InternalUserAuthInfoResponse;
import com.sixro.logistics.user.presentation.response.internal.InternalUserStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Auth Service에서 호출하는 내부 사용자 생성 API입니다.
 *
 * <p>Auth, Delivery 등 내부 서비스에서 사용자 생성,
 * 인증 정보 조회 및 사용자 검증에 사용합니다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/users")
public class UserInternalController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    /**
     * Auth Service에서 전달한 회원가입 정보를 기반으로 사용자를 생성합니다.
     */
    @PostMapping
    public CommonResponse<InternalCreateUserResponse> createUser(
            @Valid @RequestBody InternalCreateUserRequest request
    ) {
        UserResult result = userCommandService.createUser(request.toCommand());

        return CommonResponse.success(
                "사용자 생성에 성공했습니다.",
                InternalCreateUserResponse.from(result)
        );
    }

    /**
     * 로그인 검증에 필요한 사용자 인증 정보를 조회합니다.
     */
    @GetMapping("/auth-info/{username}")
    public CommonResponse<InternalUserAuthInfoResponse> getAuthInfo(
            @PathVariable String username
    ) {
        InternalUserAuthResult result =
                userQueryService.getAuthInfo(username);

        return CommonResponse.success(
                "사용자 인증 정보 조회에 성공했습니다.",
                InternalUserAuthInfoResponse.from(result)
        );
    }

    /**
     * Auth Service의 Access Token 재발급 시 필요한
     * 최신 사용자 상태, 권한 및 소속 정보를 조회합니다.
     */
    @GetMapping("/{userId}/status")
    public CommonResponse<InternalUserStatusResponse> getUserStatus(
            @PathVariable UUID userId
    ) {
        InternalUserStatusResult result =
                userQueryService.getInternalUserStatus(userId);

        return CommonResponse.success(
                "사용자 상태 조회에 성공했습니다.",
                InternalUserStatusResponse.from(result)
        );
    }

    /**
     * Delivery Service에서 배송 담당자 검증 및
     * 수령인 정보 조회에 필요한 사용자 정보를 조회합니다.
     */
    @GetMapping("/{userId}/delivery-info")
    public CommonResponse<InternalDeliveryUserResponse> getDeliveryUser(
            @PathVariable UUID userId
    ) {
        InternalDeliveryUserResult result =
                userQueryService.getDeliveryUser(userId);

        return CommonResponse.success(
                "배송 관련 사용자 정보 조회에 성공했습니다.",
                InternalDeliveryUserResponse.from(result)
        );
    }
}