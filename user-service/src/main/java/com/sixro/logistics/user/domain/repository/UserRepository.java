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

    /**
     * 삭제 여부와 관계없이 사용자를 조회합니다.
     *
     * <p>사용자 없음과 이미 비활성화된 상태를 구분할 때 사용합니다.</p>
     */
    Optional<User> findById(UUID userId);

    /**
     * 활성 사용자만 조회합니다.
     */
    Optional<User> findActiveById(UUID userId);

    Optional<User> findActiveByUsername(String username);

    boolean existsActiveByUsername(String username);

    boolean existsActiveBySlackId(String slackId);

    Page<User> search(
            UserSearchCondition condition,
            Pageable pageable
    );
}