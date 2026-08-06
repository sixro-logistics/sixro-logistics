package com.sixro.logistics.auth.domain.model;

/**
 * 사용자가 소속될 수 있는 조직 유형입니다.
 *
 * <p>가입 신청 권한에 따라 허용되는 소속 유형이 달라집니다.</p>
 */
public enum AffiliationType {

    // 허브 소속
    HUB,

    // 업체 소속
    COMPANY
}