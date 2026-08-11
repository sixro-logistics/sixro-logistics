package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.user.application.command.AdminCreateUserCommand;
import com.sixro.logistics.user.application.command.ApproveUserCommand;
import com.sixro.logistics.user.application.command.CreateUserCommand;
import com.sixro.logistics.user.application.command.DeactivateUserCommand;
import com.sixro.logistics.user.application.command.RejectUserCommand;
import com.sixro.logistics.user.application.command.UpdateUserCommand;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.application.event.UserEventPublisher;
import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.event.UserAffiliationChangedEvent;
import com.sixro.logistics.user.domain.event.UserApprovedEvent;
import com.sixro.logistics.user.domain.event.UserCreatedEvent;
import com.sixro.logistics.user.domain.event.UserDeactivatedEvent;
import com.sixro.logistics.user.domain.event.UserRejectedEvent;
import com.sixro.logistics.user.domain.event.UserRoleChangedEvent;
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
 * 사용자의 생성, 수정, 승인, 거절 및 비활성화 명령을 처리합니다.
 *
 * <p>사용자 조회 전용 로직은 {@link UserReader}에 위임하고,
 * 사용자 상태를 변경하는 비즈니스 로직과 이벤트 발행을 담당합니다.</p>
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
     * MASTER_ADMIN이 사용자를 직접 생성합니다.
     *
     * <p>일반 회원가입과 달리 생성 즉시 APPROVED 상태가 됩니다.</p>
     *
     * <p>생성 대상이 MASTER_ADMIN이면 소속 정보를 가질 수 없습니다.
     * 그 외 역할은 역할에 맞는 소속 유형과 실제 존재하는 소속 ID를
     * 가지고 있어야 합니다.</p>
     */
    public UserResult createApprovedUser(
            AdminCreateUserCommand command
    ) {
        validateMasterAdmin(
                command.requesterId(),
                command.requesterRole()
        );

        validateRoleAffiliation(
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );

        String normalizedUsername =
                command.username().trim();

        String normalizedSlackId =
                command.slackId().trim();

        validateDuplicateUsername(normalizedUsername);
        validateDuplicateSlackId(normalizedSlackId);

        /*
         * MASTER_ADMIN은 소속이 없으므로 외부 소속 서비스를
         * 호출하지 않습니다.
         *
         * 그 외 사용자는 허브 또는 업체가 실제로 존재하는지
         * 최종 확인합니다.
         */
        if (command.role() != UserRole.MASTER_ADMIN) {
            affiliationValidationService.validate(
                    command.affiliationId(),
                    command.affiliationType()
            );
        }

        User user = User.createApprovedByAdmin(
                normalizedUsername,
                command.encodedPassword(),
                normalizedSlackId,
                command.role(),
                command.affiliationId(),
                command.affiliationType(),
                command.requesterId()
        );

        User savedUser =
                userRepository.save(user);

        /*
         * 사용자 생성 자체를 알리는 이벤트입니다.
         */
        userEventPublisher.publish(
                new UserCreatedEvent(
                        savedUser.getUserId(),
                        savedUser.getUserStatus(),
                        savedUser.getRole(),
                        savedUser.getAffiliationId(),
                        savedUser.getAffiliationType()
                )
        );

        /*
         * 관리자 직접 생성 사용자는 처음부터 APPROVED 상태이므로
         * 승인 이벤트도 함께 발행합니다.
         */
        userEventPublisher.publish(
                new UserApprovedEvent(
                        savedUser.getUserId(),
                        savedUser.getUserStatus(),
                        savedUser.getRole(),
                        savedUser.getAffiliationId(),
                        savedUser.getAffiliationType(),
                        savedUser.getReviewedBy(),
                        savedUser.getReviewedAt()
                )
        );

        return UserResult.from(savedUser);
    }

    /**
     * 일반 회원가입 사용자를 생성합니다.
     *
     * <p>일반 회원가입 사용자는 PENDING 상태로 생성됩니다.</p>
     *
     * <p>가입 시점에는 역할과 소속 유형의 조합만 검증합니다.
     * 실제 허브 또는 업체의 존재 여부는 최종 승인 직전에 다시
     * 확인합니다.</p>
     */
    public UserResult createUser(
            CreateUserCommand command
    ) {
        validateRoleAffiliation(
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );

        String normalizedUsername =
                command.username().trim();

        String normalizedSlackId =
                command.slackId().trim();

        /*
         * username은 로그인 식별자이므로 논리 삭제된 사용자를
         * 포함하여 다시 사용할 수 없습니다.
         */
        validateDuplicateUsername(normalizedUsername);

        /*
         * Slack ID 역시 DB UNIQUE 정책과 맞추기 위해 논리 삭제된
         * 사용자를 포함하여 중복을 검사합니다.
         */
        validateDuplicateSlackId(normalizedSlackId);

        User user = User.create(
                normalizedUsername,
                command.encodedPassword(),
                normalizedSlackId,
                command.role(),
                command.affiliationId(),
                command.affiliationType()
        );

        User savedUser =
                userRepository.save(user);

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
     * 로그인한 사용자 또는 MASTER_ADMIN이 사용자 정보를 수정합니다.
     *
     * <p>MASTER_ADMIN은 모든 사용자의 Slack ID, 역할 및 소속을
     * 수정할 수 있습니다.</p>
     *
     * <p>일반 사용자는 자신의 Slack ID만 수정할 수 있으며,
     * PENDING 상태에서는 자신의 소속 정보도 수정할 수 있습니다.</p>
     */
    public UserResult updateUser(
            UpdateUserCommand command
    ) {
        validateUpdateFields(command);

        boolean masterRequest =
                command.requesterRole()
                        == UserRole.MASTER_ADMIN;

        /*
         * MASTER_ADMIN이 아니라면 대상 사용자를 조회하기 전에
         * 본인 요청인지 먼저 검증합니다.
         */
        if (!masterRequest) {
            validateSelfRequest(
                    command.targetUserId(),
                    command.requesterId()
            );

            /*
             * 일반 사용자는 자신의 역할을 직접 변경할 수 없습니다.
             */
            if (command.role() != null) {
                throw new BaseException(
                        UserErrorCode.USER_ACCESS_DENIED
                );
            }
        }

        User user =
                userReader.getAccessibleUser(
                        command.targetUserId()
                );

        validateDuplicateSlackId(
                command.slackId(),
                command.targetUserId()
        );

        /*
         * affiliationId와 affiliationType은 부분적으로 하나만
         * 전달할 수 없으며 함께 전달해야 합니다.
         */
        validateAffiliationFields(
                command.affiliationId(),
                command.affiliationType()
        );

        if (masterRequest) {
            return updateByMaster(user, command);
        }

        return updateBySelf(user, command);
    }

    /**
     * 수정 가능한 모든 필드가 null인 빈 PATCH 요청을 차단합니다.
     */
    private void validateUpdateFields(
            UpdateUserCommand command
    ) {
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
     * MASTER_ADMIN이 사용자의 정보를 수정합니다.
     *
     * <p>부분 변경 요청을 최종 상태로 계산한 뒤 역할과 소속의
     * 정합성을 검증합니다.</p>
     */
    private UserResult updateByMaster(
            User user,
            UpdateUserCommand command
    ) {
        UserRole previousRole =
                user.getRole();

        UUID previousAffiliationId =
                user.getAffiliationId();

        AffiliationType previousAffiliationType =
                user.getAffiliationType();

        UserRole nextRole =
                command.role() != null
                        ? command.role()
                        : user.getRole();

        UUID nextAffiliationId;
        AffiliationType nextAffiliationType;

        /*
         * MASTER_ADMIN은 허브나 업체에 소속되지 않습니다.
         *
         * 기존 사용자를 MASTER_ADMIN으로 변경하면 기존 소속
         * 정보도 함께 제거합니다.
         */
        if (nextRole == UserRole.MASTER_ADMIN) {
            if (command.affiliationId() != null
                    || command.affiliationType() != null) {
                throw new BaseException(
                        UserErrorCode.INVALID_AFFILIATION
                );
            }

            nextAffiliationId = null;
            nextAffiliationType = null;

        } else {
            /*
             * 요청에 새로운 소속 정보가 있으면 해당 값을 사용하고,
             * 없다면 기존 소속 정보를 유지합니다.
             */
            nextAffiliationId =
                    command.affiliationId() != null
                            ? command.affiliationId()
                            : user.getAffiliationId();

            nextAffiliationType =
                    command.affiliationType() != null
                            ? command.affiliationType()
                            : user.getAffiliationType();

            /*
             * 역할만 변경한 경우에도 기존 소속 유형과 새로운 역할이
             * 호환되는지 반드시 검증합니다.
             */
            validateRoleAffiliation(
                    nextRole,
                    nextAffiliationId,
                    nextAffiliationType
            );

            boolean affiliationChanged =
                    !Objects.equals(
                            previousAffiliationId,
                            nextAffiliationId
                    )
                            || previousAffiliationType
                            != nextAffiliationType;

            /*
             * 실제 소속 정보가 변경된 경우에만 Hub 또는 Company
             * Service를 호출합니다.
             */
            if (affiliationChanged) {
                affiliationValidationService.validate(
                        nextAffiliationId,
                        nextAffiliationType
                );
            }
        }

        String normalizedSlackId =
                normalizeNullableSlackId(
                        command.slackId()
                );

        /*
         * Entity에는 부분 요청값이 아니라 계산된 최종 역할과
         * 최종 소속 정보를 전달합니다.
         */
        user.updateByMaster(
                normalizedSlackId,
                nextRole,
                nextAffiliationId,
                nextAffiliationType
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
                previousAffiliationType
                        != user.getAffiliationType()
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

    /**
     * 일반 사용자가 자신의 제한된 정보를 수정합니다.
     */
    private UserResult updateBySelf(
            User user,
            UpdateUserCommand command
    ) {
        if (command.slackId() != null) {
            user.changeSlackId(
                    command.slackId().trim()
            );
        }

        boolean hasAffiliationChange =
                command.affiliationId() != null;

        if (hasAffiliationChange) {
            /*
             * 사용자가 직접 소속 정보를 변경할 수 있는 상태는
             * 가입 승인 전인 PENDING 상태뿐입니다.
             */
            if (user.getUserStatus()
                    != UserStatus.PENDING) {
                throw new BaseException(
                        UserErrorCode
                                .AFFILIATION_CHANGE_NOT_ALLOWED
                );
            }

            /*
             * 변경하려는 소속 유형이 현재 역할과 일치하는지
             * 먼저 검증합니다.
             */
            validateRoleAffiliation(
                    user.getRole(),
                    command.affiliationId(),
                    command.affiliationType()
            );

            /*
             * 실제로 존재하는 허브 또는 업체인지 확인합니다.
             */
            affiliationValidationService.validate(
                    command.affiliationId(),
                    command.affiliationType()
            );

            user.changePendingAffiliation(
                    command.affiliationId(),
                    command.affiliationType()
            );
        }

        return UserResult.from(user);
    }

    /**
     * 사용자 승인 요청을 처리합니다.
     *
     * <p>MASTER_ADMIN 또는 HUB_ADMIN만 승인할 수 있습니다.</p>
     *
     * <p>허브 관리자의 상세 관리 범위는 별도 정책이 정의되지
     * 않았으므로 현재는 역할만 확인합니다.</p>
     */
    public UserResult approveUser(
            ApproveUserCommand command,
            UserRole requesterRole
    ) {
        validateApprovalAuthority(requesterRole);

        User user =
                userReader.getAccessibleUser(
                        command.targetUserId()
                );

        /*
         * PENDING 상태의 사용자만 승인할 수 있습니다.
         */
        if (user.getUserStatus()
                != UserStatus.PENDING) {
            throw new BaseException(
                    UserErrorCode.INVALID_APPROVE_STATUS
            );
        }

        validateRoleAffiliation(
                user.getRole(),
                user.getAffiliationId(),
                user.getAffiliationType()
        );

        /*
         * 승인 직전에 소속이 여전히 존재하며 삭제되지 않았는지
         * 최종 검증합니다.
         */
        if (user.getRole()
                != UserRole.MASTER_ADMIN) {
            affiliationValidationService.validate(
                    user.getAffiliationId(),
                    user.getAffiliationType()
            );
        }

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
     * 사용자 가입 요청을 거절합니다.
     *
     * <p>거절은 소속의 존재 여부와 관계없이 처리할 수 있으므로
     * 외부 소속 서비스를 호출하지 않습니다.</p>
     */
    public UserResult rejectUser(
            RejectUserCommand command,
            UserRole requesterRole
    ) {
        validateApprovalAuthority(requesterRole);

        User user =
                userReader.getAccessibleUser(
                        command.targetUserId()
                );

        user.reject(
                command.reviewerId(),
                command.rejectedReason()
        );

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
     *
     * <p>비활성화 이벤트는 Auth Service에서 Refresh Token과
     * Session 정보를 제거하는 데 사용됩니다.</p>
     */
    public UserResult deactivateUser(
            DeactivateUserCommand command
    ) {
        validateMasterAdmin(
                command.requesterId(),
                command.requesterRole()
        );

        /*
         * MASTER_ADMIN은 자기 자신을 비활성화할 수 없습니다.
         */
        if (Objects.equals(
                command.targetUserId(),
                command.requesterId()
        )) {
            throw new BaseException(
                    UserErrorCode
                            .SELF_DEACTIVATION_NOT_ALLOWED
            );
        }

        /*
         * 논리 삭제된 사용자도 조회하여 USER_NOT_FOUND와
         * USER_ALREADY_DEACTIVATED 오류를 구분합니다.
         */
        User user =
                userReader.getById(
                        command.targetUserId()
                );

        user.deactivate(command.requesterId());

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
     * username 중복을 논리 삭제 여부와 관계없이 검사합니다.
     */
    private void validateDuplicateUsername(
            String username
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new BaseException(
                    UserErrorCode.DUPLICATE_USERNAME
            );
        }
    }

    /**
     * Slack ID 중복을 논리 삭제 여부와 관계없이 검사합니다.
     */
    private void validateDuplicateSlackId(
            String slackId
    ) {
        if (userRepository.existsBySlackId(slackId)) {
            throw new BaseException(
                    UserErrorCode.DUPLICATE_SLACK_ID
            );
        }
    }

    /**
     * 수정 대상 사용자 본인을 제외하고 Slack ID 중복을 검사합니다.
     */
    private void validateDuplicateSlackId(
            String slackId,
            UUID targetUserId
    ) {
        if (slackId == null) {
            return;
        }

        String normalizedSlackId =
                slackId.trim();

        if (userRepository.existsBySlackIdAndUserIdNot(
                normalizedSlackId,
                targetUserId
        )) {
            throw new BaseException(
                    UserErrorCode.DUPLICATE_SLACK_ID
            );
        }
    }

    /**
     * affiliationId와 affiliationType이 함께 전달됐는지 확인합니다.
     */
    private void validateAffiliationFields(
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        boolean hasAffiliationId =
                affiliationId != null;

        boolean hasAffiliationType =
                affiliationType != null;

        if (hasAffiliationId
                != hasAffiliationType) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }
    }

    /**
     * 역할과 소속 정보의 조합을 검증합니다.
     *
     * <ul>
     *     <li>MASTER_ADMIN: 소속 없음</li>
     *     <li>HUB_ADMIN: HUB 소속</li>
     *     <li>DELIVERY_MANAGER: HUB 소속</li>
     *     <li>COMPANY_MANAGER: COMPANY 소속</li>
     * </ul>
     */
    private void validateRoleAffiliation(
            UserRole role,
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        if (role == null) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }

        boolean valid = switch (role) {
            case MASTER_ADMIN ->
                    affiliationId == null
                            && affiliationType == null;

            case HUB_ADMIN, DELIVERY_MANAGER ->
                    affiliationId != null
                            && affiliationType
                            == AffiliationType.HUB;

            case COMPANY_MANAGER ->
                    affiliationId != null
                            && affiliationType
                            == AffiliationType.COMPANY;
        };

        if (!valid) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }
    }

    /**
     * null이 아닌 Slack ID를 정규화합니다.
     */
    private String normalizeNullableSlackId(
            String slackId
    ) {
        return slackId == null
                ? null
                : slackId.trim();
    }

    /**
     * 요청자가 대상 사용자 본인인지 확인합니다.
     */
    private void validateSelfRequest(
            UUID targetUserId,
            UUID requesterId
    ) {
        if (!Objects.equals(
                targetUserId,
                requesterId
        )) {
            throw new BaseException(
                    UserErrorCode.USER_ACCESS_DENIED
            );
        }
    }

    /**
     * MASTER_ADMIN 권한과 요청자 식별자를 확인합니다.
     */
    private void validateMasterAdmin(
            UUID requesterId,
            UserRole requesterRole
    ) {
        if (requesterId == null
                || requesterRole
                != UserRole.MASTER_ADMIN) {
            throw new BaseException(
                    UserErrorCode.USER_ACCESS_DENIED
            );
        }
    }

    /**
     * 가입 요청 승인 및 거절 권한을 확인합니다.
     *
     * <p>현재 확정된 요구사항에 따라 MASTER_ADMIN과 HUB_ADMIN의
     * 역할만 확인하며, 허브별 관리 범위는 제한하지 않습니다.</p>
     */
    private void validateApprovalAuthority(
            UserRole requesterRole
    ) {
        if (requesterRole
                != UserRole.MASTER_ADMIN
                && requesterRole
                != UserRole.HUB_ADMIN) {
            throw new BaseException(
                    UserErrorCode.USER_ACCESS_DENIED
            );
        }
    }
}