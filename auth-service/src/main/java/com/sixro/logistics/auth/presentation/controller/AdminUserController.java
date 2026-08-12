package com.sixro.logistics.auth.presentation.controller;

import com.sixro.logistics.auth.application.dto.AdminCreateUserResult;
import com.sixro.logistics.auth.application.service.AdminUserCommandService;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.presentation.request.AdminCreateUserRequest;
import com.sixro.logistics.auth.presentation.response.AdminCreateUserResponse;
import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Admin User",
        description = "관리자 사용자 생성 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class AdminUserController {

    private final AdminUserCommandService adminUserCommandService;

    @Operation(
            summary = "관리자 사용자 생성",
            description = "MASTER_ADMIN이 승인 완료 상태의 사용자를 생성합니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<AdminCreateUserResponse> createUser(
            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ID)
            UUID requesterId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ROLE)
            UserRole requesterRole,

            @Valid @RequestBody
            AdminCreateUserRequest request
    ) {
        AdminCreateUserResult result =
                adminUserCommandService.createUser(
                        request.toCommand(
                                requesterId,
                                requesterRole
                        )
                );

        return CommonResponse.created(
                "사용자를 생성했습니다.",
                AdminCreateUserResponse.from(result)
        );
    }
}