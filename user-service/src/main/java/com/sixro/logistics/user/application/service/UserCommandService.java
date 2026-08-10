package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
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
    private final AffiliationValidationService affiliationValidationService;

    /**
     * Auth Service에서 전달받은 회원가입 정보를 기반으로
     * <p>
     * PENDING 상태의 사용자를 생성합니다.
     *
     * <p>회원가입 시점에는 역할과 소속 유형의 조합을 검증하고,
     * 실제 허브 또는 업체의 존재 여부는 승인 시점에
     * 최종적으로 검증합니다.</p>
     */
    public UserResult createUser(CreateUserCommand command) {

        String normalizedUsername = command.username().trim();

        String normalizedSlackId = command.slackId().trim();

        /*
         * username은 로그인 식별자이므로
         * 삭제된 사용자의 username도 재사용하지 않습니다.
         */
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BaseException(
                    UserErrorCode.DUPLICATE_USERNAME
            );
        }

        /*
         * Slack ID도 DB의 UNIQUE 정책에 맞춰
         * 삭제된 사용자의 값까지 포함해 중복을 검사합니다.
         */
        if (userRepository.existsBySlackId(normalizedSlackId)) {
            throw new BaseException(
                    UserErrorCode.DUPLICATE_SLACK_ID
            );
        }

        User user = User.create(
                normalizedUsername,
                command.encodedPassword(),
                normalizedSlackId,
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
     * 사용자 정보를 부분 수정합니다.
     *
     * <p>MASTER_ADMIN은 모든 사용자의 Slack ID, 역할 및
     * 소속 정보를 수정할 수 있습니다.</p>
     *
     * <p>일반 사용자는 자신의 Slack ID를 수정할 수 있으며,
     * PENDING 상태에서는 자신의 소속 정보도 수정할 수 있습니다.</p>
     */
    public UserResult updateUser(UpdateUserCommand command) {

        validateUpdateFields(command);

        boolean masterRequest = command.requesterRole() == UserRole.MASTER_ADMIN;

        /*
         * 마스터가 아니라면 사용자 조회 전에
         * 본인 요청인지 먼저 확인합니다.
         */
        if (!masterRequest) {
            validateSelfRequest(command.targetUserId(), command.requesterId()
            );

            /*
             * 일반 사용자는 자신의 역할을 수정할 수 없습니다.
             */
            if (command.role() != null) {
                throw new BaseException(
                        UserErrorCode.USER_ACCESS_DENIED
                );
            }
        }

        User user = userReader.getAccessibleUser(command.targetUserId());

        validateDuplicateSlackId(command.slackId(), command.targetUserId()
        );

        validateAffiliationFields(command.affiliationId(), command.affiliationType());

        if (masterRequest) {
            return updateByMaster(user, command);
        }

        return updateBySelf(user, command);
    }

    /**
     * 모든 수정 필드가 null이어도 아무것도 변경하지 않고 성공 응답을 반환하는 경우를 허용하지 않습니다.
     * */
    private void validateUpdateFields(UpdateUserCommand command) {
        if (command.slackId() == null
                && command.role() == null
                && command.affiliationId() == null
                && command.affiliationType() == null) {
            throw new BaseException(
                    CommonErrorCode.INVALID_REQUEST
            );
        }
    }

    /**
     * MASTER_ADMIN이 사용자 정보를 수정합니다.
     */
    private UserResult updateByMaster(User user, UpdateUserCommand command) {
        boolean hasAffiliationChange = command.affiliationId() != null;

        UserRole nextRole = command.role() != null ? command.role() : user.getRole();

        /*
         * MASTER_ADMIN은 허브 또는 업체에 소속되지 않습니다.
         */
        if (nextRole == UserRole.MASTER_ADMIN && hasAffiliationChange) {
            throw new BaseException(UserErrorCode.INVALID_AFFILIATION);
        }

        /*
         * 새로운 소속 정보가 전달된 경우
         * 실제 존재하는 허브 또는 업체인지 확인합니다.
         */
        if (hasAffiliationChange) {
            affiliationValidationService.validate(command.affiliationId(), command.affiliationType()
            );
        }

        UserRole previousRole = user.getRole();

        UUID previousAffiliationId = user.getAffiliationId();

        AffiliationType previousAffiliationType = user.getAffiliationType();

        String normalizedSlackId = normalizeNullableSlackId(command.slackId());

        user.updateByMaster(normalizedSlackId, command.role(), command.affiliationId(), command.affiliationType());

        if (previousRole != user.getRole()) {
            userEventPublisher.publish(
                    new UserRoleChangedEvent(user.getUserId(), previousRole, user.getRole())
            );
        }

        boolean affiliationChanged =
                previousAffiliationType != user.getAffiliationType() || !Objects.equals(
                        previousAffiliationId, user.getAffiliationId()
                );

        if (affiliationChanged) {
            userEventPublisher.publish(
                    new UserAffiliationChangedEvent(
                            user.getUserId(), previousAffiliationId,
                            previousAffiliationType, user.getAffiliationId(), user.getAffiliationType()
                    )
            );
        }

        return UserResult.from(user);
    }

    /**
     * 일반 사용자가 자신의 제한된 정보를 수정합니다.
     */
    private UserResult updateBySelf(User user, UpdateUserCommand command) {
        if (command.slackId() != null) {
            user.changeSlackId(command.slackId().trim());
        }

        boolean hasAffiliationChange = command.affiliationId() != null;

        if (hasAffiliationChange) {
            /*
             * 소속 정보는 PENDING 상태에서만
             * 사용자가 직접 수정할 수 있습니다.
             */
            if (user.getUserStatus() != UserStatus.PENDING) {
                throw new BaseException(UserErrorCode.AFFILIATION_CHANGE_NOT_ALLOWED);
            }

            /*
             * 실제 존재하는 허브 또는 업체인지 확인합니다.
             */
            affiliationValidationService.validate(command.affiliationId(), command.affiliationType());

            user.changePendingAffiliation(command.affiliationId(), command.affiliationType());
        }

        return UserResult.from(user);
    }

    /**
     * 사용자 수정 시 자기 자신을 제외하고
     * Slack ID 중복을 검사합니다.
     */
    private void validateDuplicateSlackId(
            String slackId,
            UUID targetUserId
    ) {
        if (slackId == null) {
            return;
        }

        String normalizedSlackId = slackId.trim();

        if (userRepository.existsBySlackIdAndUserIdNot(normalizedSlackId, targetUserId)) {
            throw new BaseException(UserErrorCode.DUPLICATE_SLACK_ID);
        }
    }

    /**
     * affiliationId, affiliationType은 함께 전달합니다.
     *
     */
    private void validateAffiliationFields(UUID affiliationId, AffiliationType affiliationType) {
        boolean hasAffiliationId = affiliationId != null;

        boolean hasAffiliationType = affiliationType != null;

        if (hasAffiliationId != hasAffiliationType) {
            throw new BaseException(UserErrorCode.INVALID_AFFILIATION);
        }
    }

    /**
     * null이 아닌 Slack ID를 정규화합니다.
     */
    private String normalizeNullableSlackId(String slackId) {
        return slackId == null ? null : slackId.trim();
    }

    /**
     * MASTER_ADMIN 또는 HUB_ADMIN이
     * PENDING 사용자의 가입 요청을 승인합니다.
     */
    public UserResult approveUser(ApproveUserCommand command, UserRole requesterRole) {

        validateApprovalAuthority(requesterRole);

        User user = userReader.getAccessibleUser(command.targetUserId());

        // 외부 서비스 호출보다 상태 검증을 먼저 수행
        if (user.getUserStatus() != UserStatus.PENDING) {
            throw new BaseException(UserErrorCode.INVALID_APPROVE_STATUS);
        }

        // 승인 직전에 소속이 실제 존재하며 삭제되지 않았는지 최종 검증합니다.
        if (user.getRole() != UserRole.MASTER_ADMIN) {
            affiliationValidationService.validate(user.getAffiliationId(), user.getAffiliationType());
        }

        user.approve(command.reviewerId());

        userEventPublisher.publish(
                new UserApprovedEvent(
                        user.getUserId(), user.getUserStatus(), user.getRole(),
                        user.getAffiliationId(), user.getAffiliationType(),
                        user.getReviewedBy(), user.getReviewedAt()
                )
        );

        return UserResult.from(user);
    }

    /**
     * MASTER_ADMIN 또는 HUB_ADMIN이
     * PENDING 사용자의 가입 요청을 거절합니다.
     */
    public UserResult rejectUser(
            RejectUserCommand command,
            UserRole requesterRole
    ) {
        validateApprovalAuthority(requesterRole);

        User user = userReader.getAccessibleUser(command.targetUserId());

        /*
         * 거절은 존재하지 않는 소속 정보를 이유로도 처리할 수 있으므로
         * 외부 소속 조회를 실행하지 않습니다.
         */
        user.reject(command.reviewerId(), command.rejectedReason());

        userEventPublisher.publish(
                new UserRejectedEvent(
                        user.getUserId(), user.getUserStatus(),
                        user.getRejectedReason(), user.getReviewedBy(), user.getReviewedAt()
                )
        );

        return UserResult.from(user);
    }

    /**
     * 사용자를 Soft Delete 방식으로 비활성화합니다.
     */
    public UserResult deactivateUser(DeactivateUserCommand command) {

        /*
         * 사용자 비활성화는 MASTER_ADMIN만 가능합니다.
         */
        validateMasterAdmin(command.requesterRole());

        /*
         * MASTER_ADMIN도 자기 자신은 비활성화할 수 없습니다.
         */
        if (command.targetUserId().equals(command.requesterId())) {
            throw new BaseException(UserErrorCode.SELF_DEACTIVATION_NOT_ALLOWED);
        }

        /*
         * 이미 삭제된 사용자도 조회하여
         * USER_ALREADY_DEACTIVATED 오류와 USER_NOT_FOUND 오류를 구분합니다.
         */
        User user = userReader.getById(command.targetUserId());

        user.deactivate(command.requesterId());

        /*
         * Auth Service가 이 이벤트를 소비하여
         * Refresh Token과 Session을 삭제해야 합니다.
         */
        userEventPublisher.publish(
                new UserDeactivatedEvent(
                        user.getUserId(), user.getRole(),
                        user.getAffiliationId(), user.getAffiliationType(),
                        user.getDeletedBy(), user.getDeletedAt()
                )
        );

        return UserResult.from(user);
    }

    /**
     * 요청자가 대상 사용자 본인인지 확인합니다.
     */
    private void validateSelfRequest(UUID targetUserId, UUID requesterId) {
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

    /**
     * MASTER_ADMIN, HUB_ADMIN 권한인지 확인합니다.
     */
    private void validateApprovalAuthority(UserRole requesterRole) {
        if (requesterRole != UserRole.MASTER_ADMIN && requesterRole != UserRole.HUB_ADMIN) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }
}