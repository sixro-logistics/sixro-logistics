package com.sixro.logistics.user.infrastructure.persistence.repository;

import com.sixro.logistics.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA 기반의 사용자 Repository입니다.
 */
public interface JpaUserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserIdAndIsDeletedFalse(UUID userId);

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsBySlackIdAndIsDeletedFalse(String slackId);
}