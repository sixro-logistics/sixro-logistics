package com.sixro.logistics.auth.domain.model;

/**
 * 시스템에서 사용하는 사용자 권한을 정의합니다.
 *
 * <p>일반 회원가입 가능 여부와 소속 정보 검증에 사용됩니다.</p>
 */
public enum UserRole {

    /**
     * 전체 시스템을 관리하는 마스터 관리자입니다.
     *
     * <p>일반 회원가입으로는 신청할 수 없습니다.</p>
     */
    MASTER_ADMIN,

    // 담당 허브와 허브 소속 사용자를 관리하는 허브 관리자입니다.
    HUB_ADMIN,

    // 허브 배송 또는 업체 배송을 담당하는 배송 담당자입니다.
    DELIVERY_MANAGER,

    // 소속 업체와 상품을 관리하는 업체 담당자입니다.
    COMPANY_MANAGER;

    /**
     * 일반 회원가입 절차로 신청할 수 있는 권한인지 확인합니다.
     *
     * @return 일반 회원가입이 가능한 권한이면 {@code true}
     */
    public boolean isSignUpAllowed() {
        return this != MASTER_ADMIN;
    }
}