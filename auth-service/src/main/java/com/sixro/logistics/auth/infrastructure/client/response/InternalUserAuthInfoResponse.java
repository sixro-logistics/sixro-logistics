package com.sixro.logistics.auth.infrastructure.client.response;

import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;

import java.util.UUID;

/**
 * User Service에서 조회한 로그인 검증용 내부 사용자 정보입니다.
 *
 * <p>외부 클라이언트에 반환하는 응답이 아니며,
 * Auth Service의 비밀번호 및 사용자 상태 검증에만 사용합니다.</p>
 *
 * @param userId 사용자 식별자
 * @param username 사용자명
 * @param encodedPassword 암호화되어 저장된 비밀번호
 * @param role 현재 사용자 권한
 * @param userStatus 가입 승인 상태
 * @param deleted 논리 삭제 여부
 */
public record InternalUserAuthInfoResponse(
        UUID userId,
        String username,
        String encodedPassword,
        UserRole role,
        UserStatus userStatus,
        boolean deleted
) {
}