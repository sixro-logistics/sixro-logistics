package com.sixro.logistics.user.presentation.controller;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.application.service.UserCommandService;
import com.sixro.logistics.user.presentation.request.InternalCreateUserRequest;
import com.sixro.logistics.user.presentation.response.InternalCreateUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth Service에서 호출하는 내부 사용자 생성 API입니다.
 *
 * <p>Gateway에는 노출하지 않으며, 서비스 간 통신에서만 사용합니다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/users")
public class UserInternalController {

    private final UserCommandService userCommandService;

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
}