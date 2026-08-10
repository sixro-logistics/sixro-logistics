package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.user.application.dto.InternalDeliveryUserResult;
import com.sixro.logistics.user.application.dto.InternalUserAuthResult;
import com.sixro.logistics.user.application.dto.InternalUserStatusResult;
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

    /**
     * Auth Service 로그인 검증에 필요한
     * 사용자 인증 정보를 조회합니다.
     *
     * <p>가입 승인 상태는 필터링하지 않고 반환하며,
     * 로그인 허용 여부는 Auth Service에서 판단합니다.</p>
     */
    public InternalUserAuthResult getAuthInfo(String username) {
        return InternalUserAuthResult.from(
                userReader.getActiveByUsername(username)
        );
    }

    /**
     * Auth Service의 Access Token 재발급 시
     * 최신 사용자 상태, 권한 및 소속 정보를 조회합니다.
     */
    public InternalUserStatusResult getInternalUserStatus(UUID userId) {
        return InternalUserStatusResult.from(
                userReader.getAccessibleUser(userId)
        );
    }

    /**
     * Delivery Service에서 배송 담당자 검증 및 수령인 정보 조회에 필요한
     * 사용자 상태, 권한, username, Slack 및 소속 정보를 조회합니다.
     *
     * 배송 담당자 여부 등 Delivery 도메인의 세부 판단은
     * Delivery Service에서 수행합니다.
     */
    public InternalDeliveryUserResult getDeliveryUser(
            UUID userId
    ) {
        return InternalDeliveryUserResult.from(
                userReader.getAccessibleUser(userId)
        );
    }

    private void validateMasterAdmin(UserRole requesterRole) {
        if (requesterRole != UserRole.MASTER_ADMIN) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }
}