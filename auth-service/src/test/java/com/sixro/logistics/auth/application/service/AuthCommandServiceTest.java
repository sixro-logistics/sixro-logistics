package com.sixro.logistics.auth.application.service;

import com.sixro.logistics.auth.application.command.LoginCommand;
import com.sixro.logistics.auth.application.command.LogoutCommand;
import com.sixro.logistics.auth.application.command.ReissueTokenCommand;
import com.sixro.logistics.auth.application.command.SignUpCommand;
import com.sixro.logistics.auth.application.dto.SignUpResult;
import com.sixro.logistics.auth.application.dto.TokenResult;
import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.TokenPair;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;
import com.sixro.logistics.auth.domain.repository.AuthStateRepository;
import com.sixro.logistics.auth.domain.repository.RefreshTokenRepository;
import com.sixro.logistics.auth.domain.repository.SessionRepository;
import com.sixro.logistics.auth.infrastructure.client.UserServiceClient;
import com.sixro.logistics.auth.infrastructure.client.UserServiceErrorMapper;
import com.sixro.logistics.auth.infrastructure.client.request.InternalCreateUserRequest;
import com.sixro.logistics.auth.infrastructure.client.response.InternalCreateUserResponse;
import com.sixro.logistics.auth.infrastructure.client.response.InternalUserAuthInfoResponse;
import com.sixro.logistics.auth.infrastructure.client.response.InternalUserStatusResponse;
import com.sixro.logistics.auth.infrastructure.jwt.JwtClaims;
import com.sixro.logistics.auth.infrastructure.jwt.JwtProvider;
import com.sixro.logistics.auth.infrastructure.redis.TokenHashProvider;
import com.sixro.logistics.common.core.response.CommonResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 회원가입, 로그인, 토큰 재발급 및 로그아웃의 핵심 인증 정책을
 * 검증하는 Auth Application Service 단위 테스트입니다.
 *
 * <p>User Service, JWT, Redis 저장소를 모두 Mock으로 대체하여
 * 실제 네트워크나 Redis 없이 상태 검증과 저장 요청을 확인합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HUB_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final Duration ACCESS_EXPIRATION = Duration.ofMinutes(30);
    private static final Duration REFRESH_EXPIRATION = Duration.ofHours(8);

    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private UserServiceErrorMapper userServiceErrorMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AuthStateRepository authStateRepository;
    @Mock
    private TokenHashProvider tokenHashProvider;

    @InjectMocks
    private AuthCommandService authCommandService;

    @Test
    @DisplayName("회원가입 비밀번호를 암호화하여 User Service에 전달한다")
    void signUp_encodesPasswordAndCreatesUser() {
        SignUpCommand command = new SignUpCommand(
                "hubadmin1",
                "Password1!",
                "U-HUB-ADMIN",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );
        InternalCreateUserResponse response =
                new InternalCreateUserResponse(
                        USER_ID,
                        "hubadmin1",
                        UserRole.HUB_ADMIN,
                        UserStatus.PENDING
                );

        when(passwordEncoder.encode("Password1!"))
                .thenReturn("{bcrypt}encoded-password");
        when(userServiceClient.createUser(any(InternalCreateUserRequest.class)))
                .thenReturn(CommonResponse.created("사용자 생성", response));

        SignUpResult result = authCommandService.signUp(command);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.userStatus()).isEqualTo(UserStatus.PENDING);

        ArgumentCaptor<InternalCreateUserRequest> requestCaptor =
                ArgumentCaptor.forClass(InternalCreateUserRequest.class);
        verify(userServiceClient).createUser(requestCaptor.capture());
        assertThat(requestCaptor.getValue().encodedPassword())
                .isEqualTo("{bcrypt}encoded-password");
    }

    @Test
    @DisplayName("승인된 사용자가 로그인하면 새 Session과 Token Pair를 저장한다")
    void login_approvedUser_savesLoginState() {
        InternalUserAuthInfoResponse user = approvedUser();
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        when(userServiceClient.getAuthInfo("hubadmin1"))
                .thenReturn(CommonResponse.success("사용자 조회", user));
        when(passwordEncoder.matches("Password1!", user.encodedPassword()))
                .thenReturn(true);
        when(jwtProvider.issueTokenPair(
                eq(USER_ID),
                eq("hubadmin1"),
                eq(UserRole.HUB_ADMIN),
                eq(HUB_ID),
                eq(AffiliationType.HUB),
                any(UUID.class)
        )).thenReturn(tokenPair);
        when(tokenHashProvider.hash("refresh-token"))
                .thenReturn("refresh-hash");
        when(jwtProvider.getRefreshTokenExpiration())
                .thenReturn(REFRESH_EXPIRATION);
        when(jwtProvider.getAccessTokenExpiration())
                .thenReturn(ACCESS_EXPIRATION);

        TokenResult result = authCommandService.login(
                new LoginCommand("hubadmin1", "Password1!")
        );

        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.accessTokenExpiresIn()).isEqualTo(1800);

        ArgumentCaptor<UUID> issueSessionCaptor =
                ArgumentCaptor.forClass(UUID.class);
        verify(jwtProvider).issueTokenPair(
                eq(USER_ID),
                eq("hubadmin1"),
                eq(UserRole.HUB_ADMIN),
                eq(HUB_ID),
                eq(AffiliationType.HUB),
                issueSessionCaptor.capture()
        );

        ArgumentCaptor<UUID> savedSessionCaptor =
                ArgumentCaptor.forClass(UUID.class);
        verify(authStateRepository).saveLoginState(
                eq(USER_ID),
                eq("refresh-hash"),
                savedSessionCaptor.capture(),
                eq(REFRESH_EXPIRATION)
        );
        assertThat(savedSessionCaptor.getValue())
                .isEqualTo(issueSessionCaptor.getValue());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인을 거부한다")
    void login_wrongPassword_rejected() {
        InternalUserAuthInfoResponse user = approvedUser();

        when(userServiceClient.getAuthInfo("hubadmin1"))
                .thenReturn(CommonResponse.success("사용자 조회", user));
        when(passwordEncoder.matches("wrong", user.encodedPassword()))
                .thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authCommandService.login(
                        new LoginCommand("hubadmin1", "wrong")
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_USERNAME_OR_PASSWORD);
        verifyNoInteractions(jwtProvider, authStateRepository);
    }

    @ParameterizedTest
    @MethodSource("blockedLoginStatuses")
    @DisplayName("승인되지 않은 사용자는 로그인할 수 없다")
    void login_unavailableStatus_rejected(
            UserStatus status,
            AuthErrorCode expectedErrorCode
    ) {
        InternalUserAuthInfoResponse user = new InternalUserAuthInfoResponse(
                USER_ID,
                "hubadmin1",
                "{bcrypt}encoded-password",
                UserRole.HUB_ADMIN,
                status,
                HUB_ID,
                AffiliationType.HUB,
                false
        );
        when(userServiceClient.getAuthInfo("hubadmin1"))
                .thenReturn(CommonResponse.success("사용자 조회", user));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authCommandService.login(
                        new LoginCommand("hubadmin1", "Password1!")
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
        verify(passwordEncoder, never()).matches(any(), any());
        verifyNoInteractions(jwtProvider, authStateRepository);
    }

    @Test
    @DisplayName("논리 삭제된 사용자는 로그인할 수 없다")
    void login_deletedUser_rejected() {
        InternalUserAuthInfoResponse user = new InternalUserAuthInfoResponse(
                USER_ID,
                "hubadmin1",
                "{bcrypt}encoded-password",
                UserRole.HUB_ADMIN,
                UserStatus.APPROVED,
                HUB_ID,
                AffiliationType.HUB,
                true
        );
        when(userServiceClient.getAuthInfo("hubadmin1"))
                .thenReturn(CommonResponse.success("사용자 조회", user));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authCommandService.login(
                        new LoginCommand("hubadmin1", "Password1!")
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.DEACTIVATED_USER);
        verifyNoInteractions(jwtProvider, authStateRepository);
    }

    @Test
    @DisplayName("토큰 재발급은 최신 사용자 권한과 기존 Session을 사용한다")
    void reissue_usesLatestUserStateAndRotatesRefreshToken() {
        JwtClaims refreshClaims = new JwtClaims(
                USER_ID,
                UserRole.HUB_ADMIN,
                SESSION_ID,
                "old-refresh-jti",
                Instant.now().plus(REFRESH_EXPIRATION)
        );
        InternalUserStatusResponse currentUser =
                new InternalUserStatusResponse(
                        USER_ID,
                        "company1",
                        UserRole.COMPANY_MANAGER,
                        UserStatus.APPROVED,
                        COMPANY_ID,
                        AffiliationType.COMPANY
                );
        TokenPair newTokenPair =
                new TokenPair("new-access", "new-refresh");

        when(jwtProvider.parseRefreshToken("old-refresh"))
                .thenReturn(refreshClaims);
        when(sessionRepository.findSessionIdByUserId(USER_ID))
                .thenReturn(Optional.of(SESSION_ID));
        when(userServiceClient.getUserStatus(USER_ID))
                .thenReturn(CommonResponse.success("사용자 조회", currentUser));
        when(jwtProvider.issueTokenPair(
                USER_ID,
                "company1",
                UserRole.COMPANY_MANAGER,
                COMPANY_ID,
                AffiliationType.COMPANY,
                SESSION_ID
        )).thenReturn(newTokenPair);
        when(tokenHashProvider.hash("old-refresh"))
                .thenReturn("old-hash");
        when(tokenHashProvider.hash("new-refresh"))
                .thenReturn("new-hash");
        when(jwtProvider.getRefreshTokenExpiration())
                .thenReturn(REFRESH_EXPIRATION);
        when(jwtProvider.getAccessTokenExpiration())
                .thenReturn(ACCESS_EXPIRATION);
        when(authStateRepository.rotateRefreshToken(
                USER_ID,
                "old-hash",
                "new-hash",
                REFRESH_EXPIRATION
        )).thenReturn(true);

        TokenResult result = authCommandService.reissue(
                new ReissueTokenCommand("old-refresh")
        );

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        verify(jwtProvider).issueTokenPair(
                USER_ID,
                "company1",
                UserRole.COMPANY_MANAGER,
                COMPANY_ID,
                AffiliationType.COMPANY,
                SESSION_ID
        );
    }

    @Test
    @DisplayName("Refresh Token Rotation에 실패하면 재사용 토큰으로 거부한다")
    void reissue_rotationFailure_rejected() {
        JwtClaims refreshClaims = new JwtClaims(
                USER_ID,
                UserRole.HUB_ADMIN,
                SESSION_ID,
                "old-refresh-jti",
                Instant.now().plus(REFRESH_EXPIRATION)
        );
        InternalUserStatusResponse currentUser =
                new InternalUserStatusResponse(
                        USER_ID,
                        "hubadmin1",
                        UserRole.HUB_ADMIN,
                        UserStatus.APPROVED,
                        HUB_ID,
                        AffiliationType.HUB
                );

        when(jwtProvider.parseRefreshToken("old-refresh"))
                .thenReturn(refreshClaims);
        when(sessionRepository.findSessionIdByUserId(USER_ID))
                .thenReturn(Optional.of(SESSION_ID));
        when(userServiceClient.getUserStatus(USER_ID))
                .thenReturn(CommonResponse.success("사용자 조회", currentUser));
        when(jwtProvider.issueTokenPair(
                USER_ID,
                "hubadmin1",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB,
                SESSION_ID
        )).thenReturn(new TokenPair("new-access", "new-refresh"));
        when(tokenHashProvider.hash("old-refresh")).thenReturn("old-hash");
        when(tokenHashProvider.hash("new-refresh")).thenReturn("new-hash");
        when(jwtProvider.getRefreshTokenExpiration())
                .thenReturn(REFRESH_EXPIRATION);
        when(authStateRepository.rotateRefreshToken(
                USER_ID,
                "old-hash",
                "new-hash",
                REFRESH_EXPIRATION
        )).thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authCommandService.reissue(
                        new ReissueTokenCommand("old-refresh")
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃은 현재 인증 상태를 제거하고 Access Token을 블랙리스트 처리한다")
    void logout_clearsCurrentLoginState() {
        Instant accessExpiration = Instant.now().plusSeconds(600);
        JwtClaims refreshClaims = new JwtClaims(
                USER_ID,
                UserRole.HUB_ADMIN,
                SESSION_ID,
                "refresh-jti",
                Instant.now().plus(REFRESH_EXPIRATION)
        );
        JwtClaims accessClaims = new JwtClaims(
                USER_ID,
                UserRole.HUB_ADMIN,
                SESSION_ID,
                "access-jti",
                accessExpiration
        );

        when(jwtProvider.parseRefreshToken("refresh-token"))
                .thenReturn(refreshClaims);
        when(refreshTokenRepository.findTokenHashByUserId(USER_ID))
                .thenReturn(Optional.of("refresh-hash"));
        when(tokenHashProvider.hash("refresh-token"))
                .thenReturn("refresh-hash");
        when(sessionRepository.findSessionIdByUserId(USER_ID))
                .thenReturn(Optional.of(SESSION_ID));
        when(jwtProvider.parseAccessToken("access-token"))
                .thenReturn(accessClaims);

        authCommandService.logout(
                new LogoutCommand("access-token", "refresh-token")
        );

        ArgumentCaptor<Duration> ttlCaptor =
                ArgumentCaptor.forClass(Duration.class);
        verify(authStateRepository).clearLoginState(
                eq(USER_ID),
                eq("access-jti"),
                ttlCaptor.capture()
        );
        assertThat(ttlCaptor.getValue()).isPositive();
        assertThat(ttlCaptor.getValue())
                .isLessThanOrEqualTo(Duration.ofMinutes(10));
    }

    private static Stream<Arguments> blockedLoginStatuses() {
        return Stream.of(
                Arguments.of(
                        UserStatus.PENDING,
                        AuthErrorCode.USER_NOT_APPROVED
                ),
                Arguments.of(
                        UserStatus.REJECTED,
                        AuthErrorCode.USER_REJECTED
                )
        );
    }

    private InternalUserAuthInfoResponse approvedUser() {
        return new InternalUserAuthInfoResponse(
                USER_ID,
                "hubadmin1",
                "{bcrypt}encoded-password",
                UserRole.HUB_ADMIN,
                UserStatus.APPROVED,
                HUB_ID,
                AffiliationType.HUB,
                false
        );
    }
}
