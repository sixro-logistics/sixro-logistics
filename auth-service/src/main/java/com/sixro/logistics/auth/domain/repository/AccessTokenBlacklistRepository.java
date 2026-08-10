package com.sixro.logistics.auth.domain.repository;

import java.time.Duration;

/**
 * 무효화된 Access Token 저장소에 대한 추상화입니다.
 *
 * <p>로그아웃 등으로 무효화된 Access Token의 JWT ID(jti)를 저장하여
 * 토큰 만료 전 재사용되는 것을 차단합니다.</p>
 */
public interface AccessTokenBlacklistRepository {

    // Access Token의 JWT ID를 남은 유효 시간 동안 블랙리스트에 저장합니다.
    void save(
            String jwtId,
            Duration ttl
    );

    // Access Token의 JWT ID가 블랙리스트에 등록되어 있는지 확인합니다.
    boolean exists(String jwtId);

}