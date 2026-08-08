package com.sixro.logistics.auth.domain.repository;

import java.time.Duration;
import java.util.UUID;

/**
 * 인증 상태와 관련된 여러 Redis Key를
 * 하나의 원자적 연산으로 변경하기 위한 저장소 계약입니다.
 */
public interface AuthStateRepository {

    /**
     * 로그인 시 Refresh Token과 Session을 함께 저장합니다.
     */
    void saveLoginState(
            UUID userId,
            String refreshTokenHash,
            UUID sessionId,
            Duration ttl
    );

    /**
     * 로그아웃 시 Refresh Token과 Session을 삭제하고,
     * 유효한 Access Token이 있으면 blacklist에 함께 등록합니다.
     */
    void clearLoginState(
            UUID userId,
            String accessTokenJwtId,
            Duration accessTokenTtl
    );
}