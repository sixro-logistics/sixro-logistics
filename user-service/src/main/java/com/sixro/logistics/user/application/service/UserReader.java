package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 사용자 조회와 접근 가능 상태 검증을 공통으로 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;

    /**
     * 삭제 여부와 관계없이 사용자를 조회합니다.
     *
     * <p>존재하지 않는 사용자와 이미 비활성화된 사용자를
     * 구분해야 하는 유스케이스에서 사용합니다.</p>
     */
    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    }

    /**
     * 존재하며 비활성화되지 않은 사용자를 조회합니다.
     */
    public User getAccessibleUser(UUID userId) {
        User user = getById(userId);

        if (user.isDeleted()
                || user.getDeletedAt() != null
                || user.getDeletedBy() != null) {
            throw new BaseException(
                    UserErrorCode.DEACTIVATED_USER
            );
        }

        return user;
    }

    /**
     * username으로 Soft Delete되지 않은 사용자를 조회합니다.
     *
     * <p>가입 승인 상태(PENDING, APPROVED, REJECTED)는 필터링하지 않으며,
     * 로그인 허용 여부는 사용자 상태를 전달받은 Auth Service에서 판단합니다.</p>
     */
    public User getActiveByUsername(String username) {
        return userRepository.findActiveByUsername(username)
                .orElseThrow(() ->
                        new BaseException(UserErrorCode.USER_NOT_FOUND)
                );
    }
}