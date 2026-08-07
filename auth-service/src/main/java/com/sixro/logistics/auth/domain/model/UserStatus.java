package com.sixro.logistics.auth.domain.model;

/**
 * 인증 과정에서 사용하는 사용자 가입 상태입니다.
 *
 * <p>User Service의 내부 API에서 전달받은 상태를 기준으로
 * 로그인 가능 여부를 판단합니다.</p>
 */
public enum UserStatus {

    // 가입 승인 대기 상태입니다.
    PENDING,

    // 가입 승인 완료 상태입니다.
    APPROVED,

    // 가입 거절 상태입니다.
    REJECTED;

    /**
     * 현재 상태에서 로그인이 가능한지 확인합니다.
     *
     * @return APPROVED 상태이면 true
     */
    public boolean isLoginAllowed() {
        return this == APPROVED;
    }
}