package com.sixro.logistics.auth.application.dto;

import com.sixro.logistics.auth.domain.model.UserStatus;

import java.util.UUID;

/**
 * 회원가입 처리 결과를 Presentation 계층으로 전달합니다.
 *
 * <p>User Service에 사용자 생성 요청이 정상적으로 반영된 후 반환되는 결과입니다.</p>
 *
 * @param userId     생성된 사용자 식별자
 * @param username   생성된 사용자명
 * @param userStatus 최초 사용자 상태
 */
public record SignUpResult(
        UUID userId,
        String username,
        UserStatus userStatus
) {
}