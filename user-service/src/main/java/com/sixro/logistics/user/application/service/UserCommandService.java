package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.user.application.command.*;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;
import com.sixro.logistics.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 수정, 승인, 거절, 비활성화 유스케이스를 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;
    private final UserReader userReader;

    public UserResult createUser(CreateUserCommand command) {
        if (userRepository.existsActiveByUsername(command.username())) {
            throw new BaseException(UserErrorCode.DUPLICATE_USERNAME);
        }

        if (userRepository.existsActiveBySlackId(command.slackId())) {
            throw new BaseException(UserErrorCode.DUPLICATE_SLACK_ID);
        }

        User user = User.create(
                command.username(),
                command.encodedPassword(),
                command.slackId(),
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );

        User savedUser = userRepository.save(user);

        /*
         * TODO Kafka 이벤트
         * UserCreatedEvent 발행
         *
         * 회원가입 직후 상태는 PENDING이며,
         * 관리자 승인 이후 UserApprovedEvent를 별도로 발행합니다.
         */

        return UserResult.from(savedUser);
    }

    public UserResult updateUser(UpdateUserCommand command) {
        User user = userReader.getAccessibleUser(command.targetUserId());

        if (command.requesterRole() == UserRole.MASTER_ADMIN) {
            user.updateByMaster(
                    command.slackId(),
                    command.role(),
                    command.affiliationType(),
                    command.affiliationId()
            );

            /*
             * TODO Kafka 이벤트
             * 역할 또는 소속이 변경된 경우 UserRoleChangedEvent 또는
             * UserAffiliationChangedEvent 발행
             */

            return UserResult.from(user);
        }

        validateSelfRequest(command.targetUserId(), command.requesterId());

        if (command.role() != null) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }

        if (command.slackId() != null) {
            user.changeSlackId(command.slackId());
        }

        boolean hasAffiliationChange =
                command.affiliationType() != null
                        || command.affiliationId() != null;

        if (hasAffiliationChange) {
            if (user.getUserStatus() == UserStatus.APPROVED) {
                throw new BaseException(
                        UserErrorCode.APPROVED_USER_AFFILIATION_IMMUTABLE
                );
            }

            user.changePendingAffiliation(
                    command.affiliationId(),
                    command.affiliationType()
            );
        }

        return UserResult.from(user);
    }

    public UserResult approveUser(
            ApproveUserCommand command,
            UserRole requesterRole
    ) {
        validateMasterAdmin(requesterRole);

        User user = userReader.getAccessibleUser(command.targetUserId());
        user.approve(command.reviewerId());

        /*
         * TODO OpenFeign
         * 승인 전 affiliationType에 따라 Hub Service 또는 Company Service에서
         * affiliationId의 실제 존재 여부를 검증합니다.
         */

        /*
         * TODO Kafka 이벤트
         * UserApprovedEvent 발행
         * - userId
         * - role
         * - affiliationType
         * - affiliationId
         * - reviewedBy
         * - reviewedAt
         */

        return UserResult.from(user);
    }

    public UserResult rejectUser(
            RejectUserCommand command,
            UserRole requesterRole
    ) {
        validateMasterAdmin(requesterRole);

        User user = userReader.getAccessibleUser(command.targetUserId());
        user.reject(command.reviewerId(), command.rejectedReason());

        /*
         * TODO Kafka 이벤트
         * UserRejectedEvent 발행
         * - userId
         * - rejectedReason
         * - reviewedBy
         * - reviewedAt
         */

        return UserResult.from(user);
    }

    public UserResult deactivateUser(DeactivateUserCommand command) {
        User user = userReader.getById(command.targetUserId());

        if (user.isDeleted()) {
            throw new BaseException(UserErrorCode.USER_ALREADY_DEACTIVATED);
        }

        if (command.requesterRole() == UserRole.MASTER_ADMIN) {
            if (command.targetUserId().equals(command.requesterId())) {
                throw new BaseException(
                        UserErrorCode.SELF_DEACTIVATION_NOT_ALLOWED
                );
            }
        } else {
            validateSelfRequest(command.targetUserId(), command.requesterId());
        }

        user.deactivate(command.requesterId());

        /*
         * TODO Redis 연동
         * Auth Service에 대상 사용자의 Refresh Token 삭제 요청
         * Redis Key: refresh:{userId}
         */

        /*
         * TODO Access Token 즉시 무효화
         * 사용자 상태 변경 이벤트 전파, 사용자별 토큰 버전,
         * 비활성 사용자 Redis 저장 또는 Access Token 블랙리스트 검토
         */

        /*
         * TODO Kafka 이벤트
         * UserDeactivatedEvent 발행
         *
         * DB 변경과 이벤트 발행의 원자성이 필요한 경우
         * Transactional Outbox Pattern 적용 검토
         */

        return UserResult.from(user);
    }

    private void validateSelfRequest(
            java.util.UUID targetUserId,
            java.util.UUID requesterId
    ) {
        if (!targetUserId.equals(requesterId)) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }

    private void validateMasterAdmin(UserRole requesterRole) {
        if (requesterRole != UserRole.MASTER_ADMIN) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }
}