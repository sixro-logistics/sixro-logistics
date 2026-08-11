package com.sixro.logistics.user.infrastructure.persistence.repository;

import com.sixro.logistics.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA 기반의 사용자 Repository입니다.
 */
public interface JpaUserRepository extends JpaRepository<User, UUID> {

    // 로그인 등 활성 사용자 조회에 사용합니다.
    Optional<User>
    findByUsernameAndIsDeletedFalseAndDeletedAtIsNullAndDeletedByIsNull(String username);

    // 삭제 여부와 관계없는 전역 중복 검사입니다.
    boolean existsByUsername(String username);

    boolean existsBySlackId(String slackId);

    // 사용자 수정 시 자기 자신은 중복 검사에서 제외합니다.
    boolean existsBySlackIdAndUserIdNot(
            String slackId,
            UUID userId
    );
}