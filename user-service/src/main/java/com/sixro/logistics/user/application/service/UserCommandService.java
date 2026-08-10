package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.user.application.command.*;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.application.event.UserEventPublisher;
import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.event.*;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;
import com.sixro.logistics.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * 사용자 수정, 승인, 거절, 비활성화 유스케이스를 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;
    private final UserReader userReader;
    private final UserEventPublisher userEventPublisher;

    /**
     * Auth Service에서 전달받은 회원가입 정보를 기반으로 사용자를 생성합니다.
     */
    public UserResult createUser(CreateUserCommand command) {
        if (userRepository.existsActiveByUsername(command.username())) {
            throw new BaseException(UserErrorCode.DUPLICATE_USERNAME);
        }

        if (userRepository.existsActiveBySlackId(command.slackId())) {
            throw new BaseException(UserErrorCode.DUPLICATE_SLACK_ID);
        }

        /*
         * TODO(integration):
         * 회원가입 시점의 affiliationId 사전 검증 여부는 정책 확정 후 적용합니다.
         * - 가입 시점 검증은 선택, 승인 시점 검증은 필수
         *
         * 단, 최종 승인 시점에는 'Hub/Company Service 내부 API'를 통해
         * affiliationId가 실제 존재하며 삭제되지 않은 소속인지 반드시 재검증합니다.
         */

        User user = User.create(
                command.username(),
                command.encodedPassword(),
                command.slackId(),
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );

        User savedUser = userRepository.save(user);

        userEventPublisher.publish(
                new UserCreatedEvent(
                        savedUser.getUserId(),
                        savedUser.getUserStatus(),
                        savedUser.getRole(),
                        savedUser.getAffiliationId(),
                        savedUser.getAffiliationType()
                )
        );

        return UserResult.from(savedUser);
    }

    /**
     * 사용자 정보를 수정합니다.
     *
     * <p>MASTER_ADMIN은 권한과 소속을 포함한 사용자 정보를 수정할 수 있으며,
     * 일반 사용자는 자신의 제한된 정보만 수정할 수 있습니다.</p>
     */
    public UserResult updateUser(UpdateUserCommand command) {
        User user = userReader.getAccessibleUser(command.targetUserId());

        if (command.requesterRole() == UserRole.MASTER_ADMIN) {

            /*
             * TODO(integration):
             * role 또는 affiliation 정보가 변경되는 경우,
             * 변경될 소속이 실제 존재하며 삭제되지 않았는지
             * 'Hub/Company Service 내부 API'를 통해 먼저 검증합니다.
             */

            UserRole previousRole = user.getRole();
            UUID previousAffiliationId =
                    user.getAffiliationId();
            AffiliationType previousAffiliationType =
                    user.getAffiliationType();

            user.updateByMaster(
                    command.slackId(),
                    command.role(),
                    command.affiliationId(),
                    command.affiliationType()
            );

            if (previousRole != user.getRole()) {
                userEventPublisher.publish(
                        new UserRoleChangedEvent(
                                user.getUserId(),
                                previousRole,
                                user.getRole()
                        )
                );
            }

            boolean affiliationChanged =
                    previousAffiliationType != user.getAffiliationType()
                            || !Objects.equals(
                            previousAffiliationId,
                            user.getAffiliationId()
                    );

            if (affiliationChanged) {
                userEventPublisher.publish(
                        new UserAffiliationChangedEvent(
                                user.getUserId(),
                                previousAffiliationId,
                                previousAffiliationType,
                                user.getAffiliationId(),
                                user.getAffiliationType()
                        )
                );
            }

            return UserResult.from(user);
        }

        validateSelfRequest(command.targetUserId(), command.requesterId());

        /*
         * 일반 사용자는 자신의 Role을 직접 변경할 수 없습니다.
         */
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

            /*
             * TODO(integration):
             * PENDING 사용자가 affiliation 정보를 변경하는 경우,
             * 'Hub/Company Service 내부 API' 연동 후
             * 실제 존재하며 삭제되지 않은 소속인지 검증합니다.
             *
             * 승인 시점에는 최종적으로 다시 검증합니다.
             */

            user.changePendingAffiliation(
                    command.affiliationId(),
                    command.affiliationType()
            );
        }

        return UserResult.from(user);
    }

    /**
     * MASTER_ADMIN이 PENDING 사용자의 가입 요청을 승인합니다.
     */
    public UserResult approveUser(
            ApproveUserCommand command,
            UserRole requesterRole
    ) {
        validateMasterAdmin(requesterRole);

        User user = userReader.getAccessibleUser(command.targetUserId());


        /*
         * TODO(integration):
         * 승인 처리 전에 'Hub/Company Service 내부 API' 연동 후
         * affiliationId가 실제 존재하며 삭제되지 않은 소속인지 검증합니다.
         *
         * affiliationId, affiliationType 검증 성공 이후에만 APPROVED 상태로 변경합니다.
         */

        user.approve(command.reviewerId());

        userEventPublisher.publish(
                new UserApprovedEvent(
                        user.getUserId(),
                        user.getUserStatus(),
                        user.getRole(),
                        user.getAffiliationId(),
                        user.getAffiliationType(),
                        user.getReviewedBy(),
                        user.getReviewedAt()
                )
        );

        return UserResult.from(user);
    }

    /**
     * MASTER_ADMIN이 PENDING 사용자의 가입 요청을 거절합니다.
     */
    public UserResult rejectUser(
            RejectUserCommand command,
            UserRole requesterRole
    ) {
        validateMasterAdmin(requesterRole);

        User user = userReader.getAccessibleUser(command.targetUserId());
        user.reject(command.reviewerId(), command.rejectedReason());

        userEventPublisher.publish(
                new UserRejectedEvent(
                        user.getUserId(),
                        user.getUserStatus(),
                        user.getRejectedReason(),
                        user.getReviewedBy(),
                        user.getReviewedAt()
                )
        );

        return UserResult.from(user);
    }

    /**
     * 사용자를 Soft Delete 방식으로 비활성화합니다.
     */
    public UserResult deactivateUser(DeactivateUserCommand command) {

        /*
         * 이미 비활성화된 사용자인지 여부는
         * User.deactivate() 내부의 도메인 규칙에서 검증합니다.
         *
         * 따라서 삭제된 사용자도 조회할 수 있는 getById()를 사용합니다.
         */
        User user = userReader.getById(command.targetUserId());

        if (command.requesterRole() == UserRole.MASTER_ADMIN) {
            /*
             * MASTER_ADMIN 자신의 계정 비활성화는 허용하지 않습니다.
             */
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
         * 사용자 비활성화 상태는 UserDeactivatedEvent로 발행합니다.
         *
         * TODO(security):
         *  - Auth Service에서 UserDeactivatedEvent를 소비하여
         *  - Refresh Token 및 현재 로그인 Session을 무효화합니다.
         *
         * Gateway는 Redis Session 검증을 통해
         * 기존 Access Token의 재사용을 차단합니다.
         *
         * User Service는 Auth Service가 관리하는 Redis에
         * 직접 접근하지 않습니다.
         */

        userEventPublisher.publish(
                new UserDeactivatedEvent(
                        user.getUserId(),
                        user.getRole(),
                        user.getAffiliationId(),
                        user.getAffiliationType(),
                        user.getDeletedBy(),
                        user.getDeletedAt()
                )
        );

        return UserResult.from(user);
    }

    /**
     * 요청자가 대상 사용자 본인인지 확인합니다.
     */
    private void validateSelfRequest(
            java.util.UUID targetUserId,
            java.util.UUID requesterId
    ) {
        if (!targetUserId.equals(requesterId)) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }

    /**
     * MASTER_ADMIN 권한인지 확인합니다.
     */
    private void validateMasterAdmin(UserRole requesterRole) {
        if (requesterRole != UserRole.MASTER_ADMIN) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }
}