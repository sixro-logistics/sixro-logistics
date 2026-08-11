package com.sixro.logistics.auth.application.service;

import com.sixro.logistics.auth.domain.repository.RefreshTokenRepository;
import com.sixro.logistics.auth.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAuthStateInvalidationService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public void invalidate(UUID userId) {
        /*
         * Session을 먼저 삭제하면 Gateway가 기존 Access Token을
         * 즉시 차단합니다.
         */
        sessionRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);
    }
}