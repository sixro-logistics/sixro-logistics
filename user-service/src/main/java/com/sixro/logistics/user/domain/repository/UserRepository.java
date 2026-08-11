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

    /**
     * 삭제 여부와 관계없이 username 중복을 검사합니다.
     * 삭제된 사용자의 로그인 ID도 재사용하지 않습니다.
     */
    boolean existsByUsername(String username);

    // 삭제 여부와 관계없이 Slack ID 중복을 검사합니다.
    boolean existsBySlackId(String slackId);

    // 사용자 수정 시 자기 자신을 제외하고 Slack ID 중복을 검사합니다.
    boolean existsBySlackIdAndUserIdNot(String slackId, UUID excludedUserId);

    // 검색 조건에 해당하는 활성 사용자를 조회합니다.
    Page<User> search(UserSearchCondition condition, Pageable pageable);
}