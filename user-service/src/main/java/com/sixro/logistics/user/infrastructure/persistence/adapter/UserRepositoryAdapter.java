package com.sixro.logistics.user.infrastructure.persistence.adapter;

import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.repository.UserRepository;
import com.sixro.logistics.user.domain.repository.UserSearchCondition;
import com.sixro.logistics.user.infrastructure.persistence.repository.JpaUserRepository;
import com.sixro.logistics.user.infrastructure.persistence.repository.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain의 UserRepository 계약을 JPA와 QueryDSL로 구현합니다.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserQueryRepository userQueryRepository;

    @Override
    public User save(User user) {
        return jpaUserRepository.save(user);
    }

    /**
     * 삭제 여부와 관계없이 조회합니다.
     *
     * <p>비활성화 여부까지 구분해야 하는 유스케이스에서 사용합니다.</p>
     */
    @Override
    public Optional<User> findById(UUID userId) {
        return jpaUserRepository.findById(userId);
    }

    /**
     * username으로 활성 사용자를 조회합니다.
     *
     * <p>isDeleted가 false이고 deletedAt이 null인
     * 사용자만 조회합니다.</p>
     */
    @Override
    public Optional<User> findActiveByUsername(String username) {
        return jpaUserRepository.findByUsernameAndIsDeletedFalseAndDeletedAtIsNull(username);
    }

    /**
     * 삭제 여부와 관계없이 동일한 username이
     * 존재하는지 확인합니다.
     *
     * <p>삭제된 사용자의 로그인 ID도 재사용하지 않습니다.</p>
     */
    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    /**
     * 삭제 여부와 관계없이 동일한 Slack ID가
     * 존재하는지 확인합니다.
     *
     * <p>삭제된 사용자의 Slack ID도 재사용하지 않습니다.</p>
     */
    @Override
    public boolean existsBySlackId(String slackId) {
        return jpaUserRepository.existsBySlackId(slackId);
    }

    /**
     * 특정 사용자를 제외하고 동일한 Slack ID가
     * 존재하는지 확인합니다.
     *
     * <p>사용자가 자신의 기존 Slack ID를 그대로 유지하는 경우에는
     * 중복으로 판단하지 않습니다.</p>
     */
    @Override
    public boolean existsBySlackIdAndUserIdNot(String slackId, UUID excludedUserId) {
        return jpaUserRepository.existsBySlackIdAndUserIdNot(slackId, excludedUserId);
    }

    /**
     * 검색 조건에 해당하는 활성 사용자 목록을 조회합니다.
     */
    @Override
    public Page<User> search(UserSearchCondition condition, Pageable pageable) {
        return userQueryRepository.search(condition, pageable);
    }


}