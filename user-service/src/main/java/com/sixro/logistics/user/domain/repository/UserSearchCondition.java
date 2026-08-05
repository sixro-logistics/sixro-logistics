package com.sixro.logistics.user.domain.repository;

import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;

/**
 * 사용자 목록 조회의 동적 검색 조건입니다.
 *
 * <p>null인 조건은 QueryDSL 검색에서 제외합니다.</p>
 */
public record UserSearchCondition(
        String username,
        UserRole role,
        UserStatus userStatus,
        AffiliationType affiliationType
) {
}