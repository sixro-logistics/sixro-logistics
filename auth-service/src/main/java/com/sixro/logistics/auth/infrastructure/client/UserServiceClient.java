package com.sixro.logistics.auth.infrastructure.client;

import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.infrastructure.client.request.InternalAdminCreateUserRequest;
import com.sixro.logistics.auth.infrastructure.client.request.InternalCreateUserRequest;
import com.sixro.logistics.auth.infrastructure.client.response.InternalCreateUserResponse;
import com.sixro.logistics.auth.infrastructure.client.response.InternalUserAuthInfoResponse;
import com.sixro.logistics.auth.infrastructure.client.response.InternalUserStatusResponse;
import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Auth Service가 User Service의 내부 API를 호출하기 위한 OpenFeign 클라이언트입니다.
 *
 * <p>회원가입 시 사용자 생성을 요청하고,
 * 로그인 시 비밀번호와 상태 검증에 필요한 인증 정보를 조회합니다.</p>
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    // 암호화된 비밀번호를 포함한 사용자 정보를 User Service에 생성 요청합니다.
    @PostMapping("/api/v1/internal/users")
    CommonResponse<InternalCreateUserResponse> createUser(
            @RequestBody InternalCreateUserRequest request
    );

    @PostMapping("/api/v1/internal/users/admin-created")
    CommonResponse<InternalCreateUserResponse> createApprovedUser(
            @RequestHeader(HeaderConstants.USER_ID)
            UUID requesterId,

            @RequestHeader(HeaderConstants.USER_ROLE)
            UserRole requesterRole,

            @RequestBody
            InternalAdminCreateUserRequest request
    );

    // 사용자명으로 로그인 검증에 필요한 내부 인증 정보를 조회합니다.
    @GetMapping("/api/v1/internal/users/auth-info/{username}")
    CommonResponse<InternalUserAuthInfoResponse> getAuthInfo(
            @PathVariable String username
    );

    @GetMapping("/api/v1/internal/users/{userId}/status")
    CommonResponse<InternalUserStatusResponse> getUserStatus(
            @PathVariable UUID userId
    );
}