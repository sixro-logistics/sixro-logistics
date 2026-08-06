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
     * <p>존재하지 않는 사용자와 이미 비활성화된 사용자를
     * 구분해야 하는 유스케이스에서 사용합니다.</p>
     */
    @Override
    public Optional<User> findById(UUID userId) {
        return jpaUserRepository.findById(userId);
    }

    @Override
    public Optional<User> findActiveById(UUID userId) {
        return jpaUserRepository.findByUserIdAndIsDeletedFalse(userId);
    }

    @Override
    public Optional<User> findActiveByUsername(String username) {
        return jpaUserRepository.findByUsernameAndIsDeletedFalse(username);
    }

    @Override
    public boolean existsActiveByUsername(String username) {
        return jpaUserRepository.existsByUsernameAndIsDeletedFalse(username);
    }

    @Override
    public boolean existsActiveBySlackId(String slackId) {
        return jpaUserRepository.existsBySlackIdAndIsDeletedFalse(slackId);
    }

    @Override
    public Page<User> search(
            UserSearchCondition condition,
            Pageable pageable
    ) {
        return userQueryRepository.search(condition, pageable);
    }
}