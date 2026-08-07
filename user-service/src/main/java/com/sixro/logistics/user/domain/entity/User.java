package com.sixro.logistics.user.domain.entity;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 정보, 권한, 소속 및 가입 승인 상태를 관리하는 Entity입니다.
 *
 * <p>상태 변경은 Setter가 아닌 도메인 행위 메서드를 통해 수행합니다.</p>
 */
@Entity
@Getter
@Table(
        name = "p_user",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_p_user_username",
                columnNames = "username"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /**
     * BCrypt로 암호화된 비밀번호입니다.
     *
     * <p>외부 사용자 조회 API에서는 절대 반환하지 않습니다.</p>
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "slack_id", nullable = false, unique = true, length = 100)
    private String slackId;

    /**
     * PENDING 상태에서는 사용자가 신청한 권한이고,
     * APPROVED 상태에서는 관리자가 승인한 최종 권한입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    /**
     * product, hub 서비스에서 가입 승인시 검증합니다.
     */
    @Column(name = "affiliation_id")
    private UUID affiliationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "affiliation_type", length = 30)
    private AffiliationType affiliationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 30)
    private UserStatus userStatus;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            String username,
            String password,
            String slackId,
            UserRole role,
            UUID affiliationId,
            AffiliationType affiliationType,
            UserStatus userStatus
    ) {
        this.username = username;
        this.password = password;
        this.slackId = slackId;
        this.role = role;
        this.affiliationId = affiliationId;
        this.affiliationType = affiliationType;
        this.userStatus = userStatus;
    }

    /**
     * 회원가입 요청 사용자를 PENDING 상태로 생성합니다.
     */
    public static User create(
            String username,
            String encodedPassword,
            String slackId,
            UserRole requestedRole,
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        validateAffiliation(
                requestedRole,
                affiliationId,
                affiliationType
        );

        return User.builder()
                .username(username)
                .password(encodedPassword)
                .slackId(slackId)
                .role(requestedRole)
                .affiliationId(affiliationId)
                .affiliationType(affiliationType)
                .userStatus(UserStatus.PENDING)
                .build();
    }

    /**
     * 가입 요청을 승인합니다.
     */
    public void approve(UUID reviewerId) {
        validateApprovableStatus();
        validateReviewer(reviewerId);
        validateAffiliation(role, affiliationId, affiliationType);

        this.userStatus = UserStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewerId;
        this.rejectedReason = null;
    }

    /**
     * 가입 요청을 거절하고 거절 사유를 저장합니다.
     */
    public void reject(UUID reviewerId, String reason) {
        validateRejectableStatus();
        validateReviewer(reviewerId);

        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        this.userStatus = UserStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewerId;
        this.rejectedReason = reason.trim();
    }

    /**
     * Slack ID를 변경합니다.
     */
    public void changeSlackId(String slackId) {
        if (slackId == null || slackId.isBlank()) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        this.slackId = slackId.trim();
    }

    /**
     * PENDING 사용자가 신청 소속 정보를 부분 수정합니다.
     *
     * <p>null로 전달된 값은 기존 값을 유지합니다.</p>
     */
    public void changePendingAffiliation(
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        if (this.userStatus != UserStatus.PENDING) {
            throw new BaseException(
                    UserErrorCode.APPROVED_USER_AFFILIATION_IMMUTABLE
            );
        }

        AffiliationType nextAffiliationType =
                affiliationType != null
                        ? affiliationType
                        : this.affiliationType;

        UUID nextAffiliationId =
                affiliationId != null
                        ? affiliationId
                        : this.affiliationId;

        validateAffiliation(
                this.role,
                nextAffiliationId,
                nextAffiliationType
        );

        this.affiliationType = nextAffiliationType;
        this.affiliationId = nextAffiliationId;
    }

    /**
     * MASTER_ADMIN이 사용자의 권한과 소속 정보를 부분 수정합니다.
     *
     * <p>null로 전달된 값은 기존 값을 유지합니다.</p>
     */
    public void updateByMaster(
            String slackId,
            UserRole role,
            AffiliationType affiliationType,
            UUID affiliationId
    ) {
        if (slackId != null) {
            changeSlackId(slackId);
        }

        boolean hasAuthorityChange =
                role != null
                        || affiliationType != null
                        || affiliationId != null;

        if (!hasAuthorityChange) {
            return;
        }

        UserRole nextRole = role != null ? role : this.role;
        AffiliationType nextAffiliationType =
                affiliationType != null
                        ? affiliationType
                        : this.affiliationType;

        UUID nextAffiliationId =
                affiliationId != null
                        ? affiliationId
                        : this.affiliationId;

        if (nextRole == UserRole.MASTER_ADMIN) {
            nextAffiliationType = null;
            nextAffiliationId = null;
        }

        validateAffiliation(
                nextRole,
                nextAffiliationId,
                nextAffiliationType
        );

        this.role = nextRole;
        this.affiliationType = nextAffiliationType;
        this.affiliationId = nextAffiliationId;
    }

    /**
     * 사용자를 Soft Delete 방식으로 비활성화합니다.
     */
    public void deactivate(UUID deletedBy) {
        if (isDeleted()) {
            throw new BaseException(
                    UserErrorCode.USER_ALREADY_DEACTIVATED
            );
        }

        if (deletedBy == null) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }

        softDelete(deletedBy);
    }

    private void validateApprovableStatus() {
        if (this.userStatus != UserStatus.PENDING) {
            throw new BaseException(
                    UserErrorCode.INVALID_APPROVE_STATUS
            );
        }
    }

    private void validateRejectableStatus() {
        if (this.userStatus != UserStatus.PENDING) {
            throw new BaseException(
                    UserErrorCode.INVALID_REJECT_STATUS
            );
        }
    }

    private static void validateReviewer(UUID reviewerId) {
        if (reviewerId == null) {
            throw new BaseException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 권한과 소속 유형의 조합을 검증합니다.
     *
     * <p>실제 Hub 또는 Company 존재 여부는 내부 API 연동 후 추가 검증합니다.</p>
     */
    /*
     * TODO(integration):
     * Hub/Company Service 내부 API가 준비된 이후
     * affiliationId의 실제 존재 여부를 Application 계층에서 검증합니다.
     *
     * Entity에서는 role과 affiliationType 조합 규칙만 검증합니다.
     */
    private static void validateAffiliation(
            UserRole role,
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        if (role == null) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }

        if (role == UserRole.MASTER_ADMIN) {
            if (affiliationId != null || affiliationType != null) {
                throw new BaseException(
                        UserErrorCode.INVALID_AFFILIATION
                );
            }

            return;
        }

        if (affiliationId == null || affiliationType == null) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }

        boolean valid = switch (role) {
            case HUB_ADMIN, DELIVERY_MANAGER ->
                    affiliationType == AffiliationType.HUB;
            case COMPANY_MANAGER ->
                    affiliationType == AffiliationType.COMPANY;
            case MASTER_ADMIN -> false;
        };

        if (!valid) {
            throw new BaseException(
                    UserErrorCode.INVALID_AFFILIATION
            );
        }

    }
}