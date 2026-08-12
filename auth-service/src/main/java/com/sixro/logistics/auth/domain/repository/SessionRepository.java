package com.sixro.logistics.auth.domain.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자의 현재 로그인 세션 저장소에 대한 추상화입니다.
 *
 * <p>사용자당 하나의 세션만 유지하며,
 * 새로운 로그인 시 기존 sessionId를 새로운 값으로 교체합니다.</p>
 */
public interface SessionRepository {

    /**
     * 사용자의 현재 로그인 세션을 저장합니다.
     */
    void save(
            UUID userId,
            UUID sessionId,
            Duration ttl
    );

    /**
     * 사용자의 현재 유효한 sessionId를 조회합니다.
     */
    Optional<UUID> findSessionIdByUserId(UUID userId);

    /**
     * 사용자의 현재 로그인 세션을 삭제합니다.
     */
    void deleteByUserId(UUID userId);
}