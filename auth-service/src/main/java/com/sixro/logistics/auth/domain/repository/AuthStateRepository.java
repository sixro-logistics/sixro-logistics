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

    /**
     * 현재 Refresh Token hash가 요청 hash와 일치하는 경우에만
     * 새로운 Refresh Token hash로 교체하고 Session TTL을 갱신합니다.
     *
     * @return 교체에 성공하면 true, 현재 저장값이 없거나 일치하지 않으면 false
     */
    boolean rotateRefreshToken(
            UUID userId,
            String currentRefreshTokenHash,
            String newRefreshTokenHash,
            Duration ttl
    );
}