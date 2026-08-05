package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.repository.UserRepository;
import com.sixro.logistics.user.domain.repository.UserSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용자 조회 유스케이스를 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserReader userReader;

    public UserResult getMyInformation(UUID requesterId) {
        return UserResult.from(
                userReader.getAccessibleUser(requesterId)
        );
    }

    public UserResult getUser(
            UUID userId,
            UserRole requesterRole
    ) {
        validateMasterAdmin(requesterRole);

        return UserResult.from(
                userReader.getAccessibleUser(userId)
        );
    }

    public Page<UserResult> searchUsers(
            UserSearchCondition condition,
            Pageable pageable,
            UserRole requesterRole
    ) {
        validateMasterAdmin(requesterRole);

        return userRepository.search(condition, pageable)
                .map(UserResult::from);
    }

    private void validateMasterAdmin(UserRole requesterRole) {
        if (requesterRole != UserRole.MASTER_ADMIN) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }
}