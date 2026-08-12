package com.sixro.logistics.auth.application.service;

import com.sixro.logistics.auth.application.command.AdminCreateUserCommand;
import com.sixro.logistics.auth.application.dto.AdminCreateUserResult;
import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;
import com.sixro.logistics.auth.infrastructure.client.UserServiceClient;
import com.sixro.logistics.auth.infrastructure.client.UserServiceErrorMapper;
import com.sixro.logistics.auth.infrastructure.client.request.InternalAdminCreateUserRequest;
import com.sixro.logistics.auth.infrastructure.client.response.InternalCreateUserResponse;
import com.sixro.logistics.common.core.response.CommonResponse;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 마스터 관리자의 사용자 직접 생성 기능을 검증하는 단위 테스트입니다.
 *
 * <p>User Service Client와 PasswordEncoder를 Mock으로 대체하여
 * 요청자 권한, 역할·소속 조합, 비밀번호 암호화 및
 * User Service 응답·예외 처리 정책을 검증합니다.</p>
 *
 * <p>실제 User Service나 네트워크는 호출하지 않습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminUserCommandServiceTest {

    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID CREATED_USER_ID = UUID.randomUUID();
    private static final UUID HUB_ID = UUID.randomUUID();

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private UserServiceErrorMapper userServiceErrorMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserCommandService adminUserCommandService;

    @Test
    void 마스터_관리자는_승인된_허브_관리자를_생성할_수_있다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                "hubadmin1",
                "Password1!",
                "U-HUB-ADMIN",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        InternalCreateUserResponse createdUser = new InternalCreateUserResponse(
                CREATED_USER_ID,
                "hubadmin1",
                UserRole.HUB_ADMIN,
                UserStatus.APPROVED
        );

        when(passwordEncoder.encode("Password1!"))
                .thenReturn("{bcrypt}encoded-password");
        when(userServiceClient.createApprovedUser(
                eq(REQUESTER_ID),
                eq(UserRole.MASTER_ADMIN),
                any(InternalAdminCreateUserRequest.class)
        )).thenReturn(CommonResponse.created("사용자를 생성했습니다.", createdUser));

        AdminCreateUserResult result = adminUserCommandService.createUser(command);

        assertThat(result.userId()).isEqualTo(CREATED_USER_ID);
        assertThat(result.username()).isEqualTo("hubadmin1");
        assertThat(result.role()).isEqualTo(UserRole.HUB_ADMIN);
        assertThat(result.userStatus()).isEqualTo(UserStatus.APPROVED);

        ArgumentCaptor<InternalAdminCreateUserRequest> requestCaptor =
                ArgumentCaptor.forClass(InternalAdminCreateUserRequest.class);

        verify(userServiceClient).createApprovedUser(
                eq(REQUESTER_ID),
                eq(UserRole.MASTER_ADMIN),
                requestCaptor.capture()
        );

        InternalAdminCreateUserRequest internalRequest = requestCaptor.getValue();
        assertThat(internalRequest.username()).isEqualTo("hubadmin1");
        assertThat(internalRequest.encodedPassword())
                .isEqualTo("{bcrypt}encoded-password");
        assertThat(internalRequest.role()).isEqualTo(UserRole.HUB_ADMIN);
        assertThat(internalRequest.affiliationId()).isEqualTo(HUB_ID);
        assertThat(internalRequest.affiliationType()).isEqualTo(AffiliationType.HUB);
    }

    @Test
    void 마스터_관리자가_아니면_사용자를_생성할_수_없다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.HUB_ADMIN,
                "hubadmin1",
                "Password1!",
                "U-HUB-ADMIN",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> adminUserCommandService.createUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.ADMIN_USER_CREATE_FORBIDDEN);
        verifyNoInteractions(passwordEncoder, userServiceClient, userServiceErrorMapper);
    }

    @Test
    void 요청자_ID가_없으면_사용자를_생성할_수_없다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                null,
                UserRole.MASTER_ADMIN,
                "hubadmin1",
                "Password1!",
                "U-HUB-ADMIN",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> adminUserCommandService.createUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.ADMIN_USER_CREATE_FORBIDDEN);
        verifyNoInteractions(passwordEncoder, userServiceClient, userServiceErrorMapper);
    }

    @Test
    void 역할과_소속_유형이_다르면_생성을_거부한다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                "company1",
                "Password1!",
                "U-COMPANY",
                UserRole.COMPANY_MANAGER,
                HUB_ID,
                AffiliationType.HUB
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> adminUserCommandService.createUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_AFFILIATION);
        verify(passwordEncoder, never()).encode(any());
        verifyNoInteractions(userServiceClient, userServiceErrorMapper);
    }

    @Test
    void User_Service가_APPROVED가_아닌_응답을_반환하면_통신_오류로_처리한다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                "master01",
                "Password1!",
                "U-MASTER",
                UserRole.MASTER_ADMIN,
                null,
                null
        );

        InternalCreateUserResponse invalidData = new InternalCreateUserResponse(
                CREATED_USER_ID,
                "master01",
                UserRole.MASTER_ADMIN,
                UserStatus.PENDING
        );

        when(passwordEncoder.encode("Password1!"))
                .thenReturn("{bcrypt}encoded-password");
        when(userServiceClient.createApprovedUser(
                eq(REQUESTER_ID),
                eq(UserRole.MASTER_ADMIN),
                any(InternalAdminCreateUserRequest.class)
        )).thenReturn(CommonResponse.success("잘못된 응답", invalidData));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> adminUserCommandService.createUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
    }

    @Test
    void User_Service_응답이_null이면_통신_오류로_처리한다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                "master01",
                "Password1!",
                "U-MASTER",
                UserRole.MASTER_ADMIN,
                null,
                null
        );

        when(passwordEncoder.encode("Password1!"))
                .thenReturn("{bcrypt}encoded-password");
        when(userServiceClient.createApprovedUser(
                eq(REQUESTER_ID),
                eq(UserRole.MASTER_ADMIN),
                any(InternalAdminCreateUserRequest.class)
        )).thenReturn(null);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> adminUserCommandService.createUser(command)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED);
    }

    @Test
    void User_Service_Feign_예외는_오류_매퍼를_통해_변환한다() {
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                REQUESTER_ID,
                UserRole.MASTER_ADMIN,
                "master01",
                "Password1!",
                "U-MASTER",
                UserRole.MASTER_ADMIN,
                null,
                null
        );

        FeignException feignException = org.mockito.Mockito.mock(FeignException.class);
        AuthException mappedException = new AuthException(
                AuthErrorCode.DUPLICATE_USERNAME,
                feignException
        );

        when(passwordEncoder.encode("Password1!"))
                .thenReturn("{bcrypt}encoded-password");
        when(userServiceClient.createApprovedUser(
                eq(REQUESTER_ID),
                eq(UserRole.MASTER_ADMIN),
                any(InternalAdminCreateUserRequest.class)
        )).thenThrow(feignException);
        when(userServiceErrorMapper.convertUserCreateException(feignException))
                .thenReturn(mappedException);

        AuthException actual = assertThrows(
                AuthException.class,
                () -> adminUserCommandService.createUser(command)
        );

        assertThat(actual).isSameAs(mappedException);
        verify(userServiceErrorMapper).convertUserCreateException(feignException);
    }
}
