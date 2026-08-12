package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 사용자 생성, 수정, 승인, 거절 및 비활성화 명령의
 * 핵심 비즈니스 규칙을 검증하는 단위 테스트입니다.
 *
 * <p>저장소, 사용자 조회기, 소속 검증 서비스와 이벤트 발행기를
 * Mock으로 대체하여 상태 변경과 이벤트 데이터만 검증합니다.</p>
 *
 * <p>실제 DB, Kafka 또는 외부 소속 서비스는 사용하지 않습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID REVIEWER_ID = UUID.randomUUID();
    private static final UUID HUB_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private UserEventPublisher userEventPublisher;

    @Mock
    private AffiliationValidationService affiliationValidationService;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void 일반_회원가입은_값을_정규화하고_PENDING_사용자를_생성한다() {
        CreateUserCommand command = new CreateUserCommand(
                "  user01  ",
                "{bcrypt}encoded-password",
                "  U-USER-01  ",
                UserRole.DELIVERY_MANAGER,
                HUB_ID,
                AffiliationType.HUB
        );

        mockSaveWithGeneratedId();

        UserResult result = userCommandService.createUser(command);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.username()).isEqualTo("user01");
        assertThat(result.slackId()).isEqualTo("U-USER-01");
        assertThat(result.userStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(result.role()).isEqualTo(UserRole.DELIVERY_MANAGER);

        verify(userRepository).existsByUsername("user01");
        verify(userRepository).existsBySlackId("U-USER-01");
        verify(affiliationValidationService, never())
                .validate(any(), any());

        ArgumentCaptor<UserCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(eventCaptor.getValue().userStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void 일반_회원가입에서_중복_username이면_생성을_거부한다() {
        CreateUserCommand command = new CreateUserCommand(
                "user01",
                "{bcrypt}encoded-password",
                "U-USER-01",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        when(userRepository.existsByUsername("user01"))
                .thenReturn(true);

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userCommandService.createUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.DUPLICATE_USERNAME);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userEventPublisher, affiliationValidationService);
    }

    @Test
    void 마스터_관리자_직접_생성은_소속을_검증하고_생성과_승인_이벤트를_발행한다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                "hubadmin1",
                "{bcrypt}encoded-password",
                "U-HUB-ADMIN",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        mockSaveWithGeneratedId();

        UserResult result = userCommandService.createApprovedUser(command);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.userStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(result.reviewedBy()).isEqualTo(REQUESTER_ID);

        verify(affiliationValidationService)
                .validate(HUB_ID, AffiliationType.HUB);
        verify(userEventPublisher).publish(any(UserCreatedEvent.class));

        ArgumentCaptor<UserApprovedEvent> approvedEventCaptor =
                ArgumentCaptor.forClass(UserApprovedEvent.class);
        verify(userEventPublisher).publish(approvedEventCaptor.capture());
        assertThat(approvedEventCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(approvedEventCaptor.getValue().reviewedBy())
                .isEqualTo(REQUESTER_ID);
    }

    @Test
    void 마스터_관리자가_아니면_직접_생성할_수_없다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.HUB_ADMIN,
                "hubadmin1",
                "{bcrypt}encoded-password",
                "U-HUB-ADMIN",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userCommandService.createApprovedUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.USER_ACCESS_DENIED);
        verifyNoInteractions(
                userRepository,
                userReader,
                userEventPublisher,
                affiliationValidationService
        );
    }

    @Test
    void PENDING_사용자는_자신의_소속을_변경할_수_있다() {
        UUID newHubId = UUID.randomUUID();
        User user = pendingUser(
                UserRole.DELIVERY_MANAGER,
                HUB_ID,
                AffiliationType.HUB
        );

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                USER_ID,
                UserRole.DELIVERY_MANAGER,
                null,
                null,
                newHubId,
                AffiliationType.HUB
        );

        when(userReader.getAccessibleUser(USER_ID)).thenReturn(user);

        UserResult result = userCommandService.updateUser(command);

        assertThat(result.affiliationId()).isEqualTo(newHubId);
        assertThat(result.affiliationType()).isEqualTo(AffiliationType.HUB);
        verify(affiliationValidationService)
                .validate(newHubId, AffiliationType.HUB);
    }

    @Test
    void APPROVED_사용자는_자신의_소속을_변경할_수_없다() {
        User user = approvedUser(
                UserRole.DELIVERY_MANAGER,
                HUB_ID,
                AffiliationType.HUB
        );
        UUID newHubId = UUID.randomUUID();

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                USER_ID,
                UserRole.DELIVERY_MANAGER,
                null,
                null,
                newHubId,
                AffiliationType.HUB
        );

        when(userReader.getAccessibleUser(USER_ID)).thenReturn(user);

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userCommandService.updateUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.AFFILIATION_CHANGE_NOT_ALLOWED);
        verify(affiliationValidationService, never()).validate(any(), any());
    }

    @Test
    void 마스터가_역할과_소속을_변경하면_두_변경_이벤트를_발행한다() {
        User user = approvedUser(
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        UpdateUserCommand command = new UpdateUserCommand(
                USER_ID,
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                null,
                UserRole.COMPANY_MANAGER,
                COMPANY_ID,
                AffiliationType.COMPANY
        );

        when(userReader.getAccessibleUser(USER_ID)).thenReturn(user);

        UserResult result = userCommandService.updateUser(command);

        assertThat(result.role()).isEqualTo(UserRole.COMPANY_MANAGER);
        assertThat(result.affiliationId()).isEqualTo(COMPANY_ID);
        assertThat(result.affiliationType()).isEqualTo(AffiliationType.COMPANY);

        verify(affiliationValidationService)
                .validate(COMPANY_ID, AffiliationType.COMPANY);

        ArgumentCaptor<UserRoleChangedEvent> roleEventCaptor =
                ArgumentCaptor.forClass(UserRoleChangedEvent.class);
        verify(userEventPublisher).publish(roleEventCaptor.capture());
        assertThat(roleEventCaptor.getValue().previousRole())
                .isEqualTo(UserRole.HUB_ADMIN);
        assertThat(roleEventCaptor.getValue().newRole())
                .isEqualTo(UserRole.COMPANY_MANAGER);

        ArgumentCaptor<UserAffiliationChangedEvent> affiliationEventCaptor =
                ArgumentCaptor.forClass(UserAffiliationChangedEvent.class);
        verify(userEventPublisher).publish(affiliationEventCaptor.capture());
        assertThat(affiliationEventCaptor.getValue().previousAffiliationId())
                .isEqualTo(HUB_ID);
        assertThat(affiliationEventCaptor.getValue().newAffiliationId())
                .isEqualTo(COMPANY_ID);
    }

    @Test
    void 사용자_승인_직전에_실제_소속을_검증하고_승인_이벤트를_발행한다() {
        User user = pendingUser(
                UserRole.COMPANY_MANAGER,
                COMPANY_ID,
                AffiliationType.COMPANY
        );
        ApproveUserCommand command = new ApproveUserCommand(USER_ID, REVIEWER_ID);

        when(userReader.getAccessibleUser(USER_ID)).thenReturn(user);

        UserResult result = userCommandService.approveUser(
                command,
                UserRole.HUB_ADMIN
        );

        assertThat(result.userStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(result.reviewedBy()).isEqualTo(REVIEWER_ID);
        verify(affiliationValidationService)
                .validate(COMPANY_ID, AffiliationType.COMPANY);

        ArgumentCaptor<UserApprovedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserApprovedEvent.class);
        verify(userEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userStatus())
                .isEqualTo(UserStatus.APPROVED);
    }

    @Test
    void 관리자가_PENDING_사용자를_거절하면_사유를_정규화하고_거절_이벤트를_발행한다() {
        User user = pendingUser(
                UserRole.COMPANY_MANAGER,
                COMPANY_ID,
                AffiliationType.COMPANY
        );
        RejectUserCommand command = new RejectUserCommand(
                USER_ID,
                REVIEWER_ID,
                "  가입 정보가 일치하지 않습니다.  "
        );

        when(userReader.getAccessibleUser(USER_ID)).thenReturn(user);

        UserResult result = userCommandService.rejectUser(
                command,
                UserRole.MASTER_ADMIN
        );

        assertThat(result.userStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(result.rejectedReason())
                .isEqualTo("가입 정보가 일치하지 않습니다.");
        assertThat(result.reviewedBy()).isEqualTo(REVIEWER_ID);
        assertThat(result.reviewedAt()).isNotNull();

        verifyNoInteractions(affiliationValidationService);

        ArgumentCaptor<UserRejectedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserRejectedEvent.class);
        verify(userEventPublisher).publish(eventCaptor.capture());

        UserRejectedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.userStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(event.rejectedReason())
                .isEqualTo("가입 정보가 일치하지 않습니다.");
        assertThat(event.reviewedBy()).isEqualTo(REVIEWER_ID);
        assertThat(event.reviewedAt()).isNotNull();
    }

    @Test
    void 승인_권한이_없는_사용자는_가입_요청을_거절할_수_없다() {
        RejectUserCommand command = new RejectUserCommand(
                USER_ID,
                REVIEWER_ID,
                "권한 없는 거절 요청"
        );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userCommandService.rejectUser(
                        command,
                        UserRole.COMPANY_MANAGER
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.USER_ACCESS_DENIED);
        verifyNoInteractions(
                userReader,
                userEventPublisher,
                affiliationValidationService
        );
    }

    @Test
    void 마스터가_사용자를_비활성화하면_삭제자까지_포함한_이벤트를_발행한다() {
        User user = approvedUser(
                UserRole.DELIVERY_MANAGER,
                HUB_ID,
                AffiliationType.HUB
        );
        DeactivateUserCommand command = new DeactivateUserCommand(
                USER_ID,
                REQUESTER_ID,
                UserRole.MASTER_ADMIN
        );

        when(userReader.getById(USER_ID)).thenReturn(user);

        UserResult result = userCommandService.deactivateUser(command);

        assertThat(result.deletedAt()).isNotNull();
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedBy()).isEqualTo(REQUESTER_ID);

        ArgumentCaptor<UserDeactivatedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserDeactivatedEvent.class);
        verify(userEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(eventCaptor.getValue().deletedBy()).isEqualTo(REQUESTER_ID);
        assertThat(eventCaptor.getValue().deletedAt()).isNotNull();
    }

    @Test
    void 마스터는_자기_자신을_비활성화할_수_없다() {
        DeactivateUserCommand command = new DeactivateUserCommand(
                REQUESTER_ID,
                REQUESTER_ID,
                UserRole.MASTER_ADMIN
        );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userCommandService.deactivateUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.SELF_DEACTIVATION_NOT_ALLOWED);
        verifyNoInteractions(userReader, userEventPublisher);
    }

    private void mockSaveWithGeneratedId() {
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(user, "userId", USER_ID);
                    return user;
                });
    }

    private User pendingUser(
            UserRole role,
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        User user = User.create(
                "user01",
                "{bcrypt}encoded-password",
                "U-USER-01",
                role,
                affiliationId,
                affiliationType
        );
        ReflectionTestUtils.setField(user, "userId", USER_ID);
        return user;
    }

    private User approvedUser(
            UserRole role,
            UUID affiliationId,
            AffiliationType affiliationType
    ) {
        User user = User.createApprovedByAdmin(
                "user01",
                "{bcrypt}encoded-password",
                "U-USER-01",
                role,
                affiliationId,
                affiliationType,
                REVIEWER_ID
        );
        ReflectionTestUtils.setField(user, "userId", USER_ID);
        return user;
    }
}
