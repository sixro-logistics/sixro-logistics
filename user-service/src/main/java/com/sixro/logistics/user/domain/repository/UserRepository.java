package com.sixro.logistics.user.domain.repository;

import com.sixro.logistics.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * User 도메인의 영속성 계약입니다.
 */
public interface UserRepository {

    User save(User user);

    // 삭제 여부와 관계없이 사용자를 조회합니다.
    Optional<User> findById(UUID userId);

    // username으로 Soft Delete되지 않은 사용자를 조회합니다.
    Optional<User> findActiveByUsername(String username);

    // 동일한 username을 사용하는 활성 사용자가 존재하는지 확인합니다.
    boolean existsActiveByUsername(String username);

    // 동일한 Slack ID를 사용하는 활성 사용자가 존재하는지 확인합니다.
    boolean existsActiveBySlackId(String slackId);

    // 검색 조건에 해당하는 활성 사용자를 조회합니다.
    Page<User> search(
            UserSearchCondition condition,
            Pageable pageable
    );
}