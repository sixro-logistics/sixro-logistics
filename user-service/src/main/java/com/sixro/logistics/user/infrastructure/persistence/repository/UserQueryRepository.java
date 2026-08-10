package com.sixro.logistics.user.infrastructure.persistence.repository;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.repository.UserSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * QueryDSL 기반 사용자 목록 검색 계약입니다.
 */
public interface UserQueryRepository {

    Page<User> search(
            UserSearchCondition condition,
            Pageable pageable
    );
}