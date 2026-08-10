package com.sixro.logistics.auth.domain.repository;

import com.sixro.logistics.auth.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token 저장소에 대한 추상화입니다.
 *
 * <p>Refresh Token 원문이 아닌 해시값을 저장하며,
 * 사용자별 현재 유효한 Refresh Token을 관리합니다.</p>
 */
public interface RefreshTokenRepository {

    // 사용자의 Refresh Token 해시값을 만료 시간과 함께 저장합니다.
    void save(RefreshToken refreshToken);

    // 사용자 ID로 저장된 Refresh Token 해시값을 조회합니다.
    Optional<String> findTokenHashByUserId(UUID userId);

    // 사용자 ID에 해당하는 Refresh Token을 삭제합니다.
    void deleteByUserId(UUID userId);

}