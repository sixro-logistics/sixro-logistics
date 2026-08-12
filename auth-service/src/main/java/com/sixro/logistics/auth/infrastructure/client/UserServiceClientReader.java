package com.sixro.logistics.auth.infrastructure.client;

import com.sixro.logistics.auth.infrastructure.client.response.InternalUserAuthInfoResponse;
import com.sixro.logistics.auth.infrastructure.client.response.InternalUserStatusResponse;
import com.sixro.logistics.common.core.response.CommonResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Auth Service에서 인증에 필요한 사용자 정보를 조회합니다.
 *
 * <p>User Service 장애가 반복될 때 호출을 계속 전달하지 않도록
 * 조회 API에 Circuit Breaker를 적용합니다.</p>
 *
 <p>이 컴포넌트는 로그인 및 토큰 재발급에 사용하는
 조회 API만 Circuit Breaker 보호 대상으로 관리합니다.</p>

 <p>사용자 생성 POST 요청은 별도로 분리하며,
 중복 생성 가능성이 있으므로 자동 Retry를 적용하지 않습니다.</p>
 */
@Component
@RequiredArgsConstructor
public class UserServiceClientReader {

    private final UserServiceClient userServiceClient;

    /**
     * 로그인에 필요한 사용자 인증 정보를 조회합니다.
     */
    @CircuitBreaker(name = "authUserService")
    public CommonResponse<InternalUserAuthInfoResponse> getAuthInfo(
            String username
    ) {
        return userServiceClient.getAuthInfo(username);
    }

    /**
     * 토큰 재발급에 필요한 최신 사용자 상태를 조회합니다.
     */
    @CircuitBreaker(name = "authUserService")
    public CommonResponse<InternalUserStatusResponse> getUserStatus(
            UUID userId
    ) {
        return userServiceClient.getUserStatus(userId);
    }
}