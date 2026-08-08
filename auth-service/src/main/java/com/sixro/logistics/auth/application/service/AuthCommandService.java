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
import com.sixro.logistics.auth.domain.model.RefreshToken;
import com.sixro.logistics.auth.domain.model.TokenPair;
import com.sixro.logistics.auth.domain.model.UserStatus;
import com.sixro.logistics.auth.domain.repository.AccessTokenBlacklistRepository;
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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 회원가입, 로그인, 토큰 재발급 및 로그아웃 유스케이스를 처리합니다.
 *
 *
 * <p>사용자 원본 정보는 User Service가 관리하며,
 * Auth Service는 인증에 필요한 정보만 내부 API로 조회합니다.</p>
 *
 * <p>주요 책임은 다음과 같습니다.</p>
 *
 * <ul>
 *     <li>User Service를 통한 사용자 생성 및 인증 정보 조회</li>
 *     <li>비밀번호 암호화 및 일치 여부 검증</li>
 *     <li>Access Token과 Refresh Token 발급</li>
 *     <li>Refresh Token 해시 저장 및 검증</li>
 *     <li>사용자별 단일 로그인 세션 관리</li>
 *     <li>토큰 재발급 시 최신 사용자 상태 및 권한 검증</li>
 *     <li>로그아웃 시 Refresh Token / Session 폐기</li>
 *     <li>유효한 Access Token의 블랙리스트 등록</li>
 * </ul>
 *
 */
@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserServiceClient userServiceClient;
    private final UserServiceErrorMapper userServiceErrorMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final SessionRepository sessionRepository;
    private final AuthStateRepository authStateRepository;

    private final TokenHashProvider tokenHashProvider;

    /**
     * 회원가입 정책을 검증하고 User Service에 사용자 생성을 요청합니다.
     *
     * <p>비밀번호는 Auth Service에서 BCrypt로 암호화한 후 전달합니다.
     * 원문 비밀번호는 User Service에 전달하거나 저장하지 않습니다.</p>
     */
    public SignUpResult signUp(SignUpCommand command) {
        validateSignUp(command);

        String encodedPassword = passwordEncoder.encode(command.password());

        InternalCreateUserResponse response =
                createUser(command, encodedPassword);

        return new SignUpResult(
                response.userId(),
                response.username(),
                response.userStatus()
        );
    }

    /**
     * 사용자 인증 정보를 검증하고 새로운 로그인 세션을 생성합니다.
     *
     * <p>로그인할 때마다 새로운 sessionId를 생성합니다.
     * Redis에는 사용자당 하나의 sessionId만 저장하므로,
     * 동일 사용자가 다시 로그인하면 기존 세션은 새로운 세션으로 교체됩니다.</p>
     *
     * <p>Access Token과 Refresh Token에는 동일한 sessionId를 포함합니다.
     * Gateway에서 JWT의 sessionId와 Redis의 현재 sessionId를 비교하면
     * 이전 로그인에서 발급된 Access Token도 즉시 차단할 수 있습니다.</p>
     */
    public TokenResult login(LoginCommand command) {
        InternalUserAuthInfoResponse user =
                getAuthInfo(command.username());

        validateLoginUser(user);

        if (!passwordEncoder.matches(
                command.password(),
                user.encodedPassword()
        )) {
            // TODO(auth): 사용자 또는 IP 기준 로그인 실패 횟수를 Redis에 기록하고
            //  - 임계치 초과 시 일정 시간 로그인을 제한하는 정책을 검토합니다.
            throw new AuthException(
                    AuthErrorCode.INVALID_USERNAME_OR_PASSWORD
            );
        }

        /*
         * 새로운 로그인 세션을 생성합니다.
         *
         * 기존 session:{userId}가 존재하더라도 새로운 값으로 교체되므로
         * 이전 로그인 세션은 더 이상 현재 세션으로 인정되지 않습니다.
         */
        UUID sessionId = UUID.randomUUID();

        TokenPair tokenPair = jwtProvider.issueTokenPair(
                user.userId(),
                user.role(),
                sessionId
        );

        String refreshTokenHash =
                tokenHashProvider.hash(
                        tokenPair.refreshToken()
                );

        authStateRepository.saveLoginState(
                user.userId(),
                refreshTokenHash,
                sessionId,
                jwtProvider.getRefreshTokenExpiration()
        );

        return toTokenResult(tokenPair);
    }

    /**
     * Refresh Token을 검증하고 새로운 Token Pair를 발급합니다.
     *
     * <p>Refresh Token에 저장되어 있던 과거 role을 신뢰하지 않고,
     * User Service에서 현재 사용자 상태와 권한을 다시 조회합니다.</p>
     *
     * <p>따라서 Refresh Token 발급 이후 사용자의 권한이 변경되었더라도
     * 최신 권한을 기준으로 새로운 Access Token을 발급합니다.</p>
     */
    public TokenResult reissue(
            ReissueTokenCommand command
    ) {

        /*
         * 1. Refresh Token 자체를 검증합니다.
         *
         * - RSA 서명
         * - issuer
         * - expiration
         * - tokenType == REFRESH
         */
        JwtClaims refreshClaims = jwtProvider.parseRefreshToken(
                command.refreshToken()
        );

        /*
         * 2. Redis에 저장된 Refresh Token hash와
         * 요청받은 Refresh Token의 hash가 일치하는지 확인합니다.
         *
         * 이미 Rotation된 이전 Refresh Token은 여기서 차단됩니다.
         */
        validateSavedRefreshToken(
                refreshClaims.userId(),
                command.refreshToken()
        );

        /*
         * 3. Refresh Token이 현재 로그인 세션에 속하는지 확인합니다.
         *
         * 동일 사용자가 새로 로그인하여 sessionId가 변경된 경우
         * 이전 Refresh Token은 사용할 수 없습니다.
         */
        validateCurrentSession(
                refreshClaims.userId(),
                refreshClaims.sessionId()
        );

        /*
         * 4. User Service에서 최신 사용자 상태,
         * 권한 및 소속 정보를 조회합니다.
         */
        InternalUserStatusResponse user =
                getUserStatus(refreshClaims.userId());

        /*
         * 5. 현재 로그인 가능한 사용자 상태인지 확인합니다.
         */
        validateReissueUser(user);

        /*
         * 6. 최신 사용자 권한으로 Token Pair를 다시 발급합니다.
         *
         * 재발급은 새로운 로그인이 아니므로
         * 기존 sessionId는 그대로 유지합니다.
         *
         * Refresh Token에 포함되어 있던 claims.role()은
         * 새로운 Token 발급 권한으로 사용하지 않습니다.
         */
        TokenPair newTokenPair =
                jwtProvider.issueTokenPair(
                        user.userId(),
                        user.role(),
                        refreshClaims.sessionId()
                );

        /*
         * 7. Refresh Token Rotation
         *
         * 새로운 Refresh Token hash로 기존 Redis 값을 교체하여
         * 이전 Refresh Token을 다시 사용할 수 없도록 합니다.
         */
        saveRefreshToken(
                user.userId(),
                newTokenPair.refreshToken()
        );

        /*
         * 새로운 Refresh Token의 만료 시간이 다시 설정되었으므로
         * 동일한 sessionId의 TTL도 Refresh Token 만료시간에 맞춰 갱신합니다.
         */
        saveSession(
                user.userId(),
                refreshClaims.sessionId()
        );

        /*
         * TODO(auth):
         * Refresh Token 검증과 Rotation을 서로 분리된 Redis 연산으로 처리하면
         * 동시에 동일 Refresh Token으로 재발급 요청이 들어오는 경우
         * Race Condition이 발생할 수 있습니다.
         *
         * 추후 Redis Lua Script 또는 원자적 Compare-And-Set 방식으로
         * 검증과 교체를 하나의 연산으로 처리하는 것을 검토합니다.
         *
         * 이미 Rotation된 Refresh Token의 재사용을 감지한 경우
         * 탈취 가능성을 고려하여 사용자 세션 전체를 폐기하는 정책도 검토합니다.
         */

        return toTokenResult(newTokenPair);
    }

    /**
     * Refresh Token을 기준으로 현재 로그인 세션을 폐기합니다.
     *
     * <p>로그아웃의 핵심은 Refresh Token과 현재 Session을 폐기하는 것입니다.</p>
     *
     * <p>Access Token이 이미 만료되었더라도 로그아웃은 정상 처리하며,
     * 아직 유효한 Access Token만 남은 수명 동안 블랙리스트에 등록합니다.</p>
     */
    public void logout(LogoutCommand command) {

        /*
         * 1. Refresh Token부터 검증합니다.
         *
         * Access Token이 만료된 경우에도 Refresh Token을 폐기할 수 있도록
         * 로그아웃 기준 토큰은 Refresh Token으로 처리합니다.
         */
        JwtClaims refreshClaims =
                jwtProvider.parseRefreshToken(
                        command.refreshToken()
                );

        /*
         * 2. Redis에 저장된 현재 Refresh Token인지 확인합니다.
         */
        validateSavedRefreshToken(
                refreshClaims.userId(),
                command.refreshToken()
        );

        /*
         * 3. 현재 로그인 세션에 속하는 Refresh Token인지 확인합니다.
         */
        validateCurrentSession(
                refreshClaims.userId(),
                refreshClaims.sessionId()
        );

        /*
         * 4. Access Token 상태를 확인합니다.
         *
         * 아직 유효한 경우에는 같은 사용자와 같은 세션에서
         * 발급된 토큰인지 검증합니다.
         *
         * 만료된 Access Token은 블랙리스트 등록 대상이 아니므로
         * null을 반환하고 로그아웃을 계속 진행합니다.
         */
        JwtClaims accessClaims =
                parseAccessTokenForLogout(
                        command.accessToken(),
                        refreshClaims
                );

        String accessTokenJwtId = null;
        Duration accessTokenTtl = null;

        if (accessClaims != null) {
            Duration remaining = Duration.between(
                    Instant.now(),
                    accessClaims.expiration()
            );

            if (!remaining.isNegative()
                    && !remaining.isZero()) {
                accessTokenJwtId =
                        accessClaims.jwtId();

                accessTokenTtl =
                        remaining;
            }
        }

        authStateRepository.clearLoginState(
                refreshClaims.userId(),
                accessTokenJwtId,
                accessTokenTtl
        );
    }

    /**
     * 로그아웃 요청에 포함된 Access Token을 확인합니다.
     *
     * <p>유효한 Access Token이면 Refresh Token과
     * 동일 사용자 및 동일 로그인 세션에서 발급되었는지 검증합니다.</p>
     *
     * <p>Access Token이 이미 만료되었다면
     * 별도의 blacklist 처리가 필요하지 않으므로 null을 반환합니다.</p>
     */
    private JwtClaims parseAccessTokenForLogout(
            String accessToken,
            JwtClaims refreshClaims
    ) {
        try {
            JwtClaims accessClaims =
                    jwtProvider.parseAccessToken(accessToken);

            validateSameTokenOwner(
                    refreshClaims,
                    accessClaims
            );

            return accessClaims;

        } catch (AuthException exception) {
            /*
             * Access Token 만료는 로그아웃 실패 조건이 아닙니다.
             *
             * Refresh Token 검증과 Session 검증이 정상적으로 완료되었다면
             * Access Token blacklist 등록만 생략하고 로그아웃을 계속합니다.
             */
            if (exception.getErrorCode()
                    == AuthErrorCode.EXPIRED_ACCESS_TOKEN) {
                return null;
            }

            throw exception;
        }
    }

    /**
     * User Service에 사용자 생성을 요청합니다.
     *
     * <p>User Service 호출 과정에서 발생한 Feign 오류는
     * Auth Service의 오류 코드로 변환합니다.</p>
     */
    private InternalCreateUserResponse createUser(
            SignUpCommand command,
            String encodedPassword
    ) {
        try {
            CommonResponse<InternalCreateUserResponse> response =
                    userServiceClient.createUser(
                            InternalCreateUserRequest.from(
                                    command,
                                    encodedPassword
                            )
                    );

            return requireData(response);

        } catch (FeignException exception) {
            throw userServiceErrorMapper.convertSignUpException(
                    exception
            );
        }
    }

    /**
     * 로그인을 위해 User Service에서 사용자 인증 정보를 조회합니다.
     *
     * <p>사용자 존재 여부가 외부에 노출되지 않도록
     * User Service의 404 응답은 아이디 또는 비밀번호 불일치로 변환합니다.</p>
     */
    private InternalUserAuthInfoResponse getAuthInfo(
            String username
    ) {
        try {
            CommonResponse<InternalUserAuthInfoResponse> response =
                    userServiceClient.getAuthInfo(username);

            return requireData(response);

        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new AuthException(
                        AuthErrorCode.INVALID_USERNAME_OR_PASSWORD,
                        exception
                );
            }

            throw new AuthException(
                    AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED,
                    exception
            );
        }
    }

    /**
     * Token 재발급에 필요한 최신 사용자 상태와 권한 정보를
     * User Service에서 조회합니다.
     */
    private InternalUserStatusResponse getUserStatus(
            UUID userId
    ) {
        try {
            CommonResponse<InternalUserStatusResponse> response =
                    userServiceClient.getUserStatus(userId);

            return requireData(response);

        } catch (FeignException exception) {
            switch (exception.status()) {
                /*
                 * User Service에서 사용자를 찾을 수 없는 경우(U001),
                 * 더 이상 해당 사용자에 대한 Refresh Token을
                 * 유효한 인증 수단으로 인정하지 않습니다.
                 */
                case 404 -> throw new AuthException(
                        AuthErrorCode.INVALID_REFRESH_TOKEN,
                        exception
                );

                /*
                 * User Service에서 비활성화된 사용자(U002)로 판단한 경우
                 * Auth Service에서도 비활성 사용자 오류로 변환합니다.
                 */
                case 410 -> throw new AuthException(
                        AuthErrorCode.DEACTIVATED_USER,
                        exception
                );

                /*
                 * 그 외 User Service 오류 및 통신 문제는
                 * 내부 서비스 통신 실패로 처리합니다.
                 */
                default -> throw new AuthException(
                        AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED,
                        exception
                );
            }
        }
    }

    /**
     * Token 재발급 대상 사용자가
     * 현재 로그인 가능한 상태인지 검증합니다.
     */
    private void validateReissueUser(
            InternalUserStatusResponse user
    ) {
        if (user.userStatus() == UserStatus.REJECTED) {
            throw new AuthException(
                    AuthErrorCode.USER_REJECTED
            );
        }

        if (!user.userStatus().isLoginAllowed()) {
            throw new AuthException(
                    AuthErrorCode.USER_NOT_APPROVED
            );
        }
    }

    /**
     * 가입 가능한 권한인지 확인하고
     * Role과 affiliation 정보의 조합을 검증합니다.
     *
     * <p>현재 회원가입 정책은 다음과 같습니다.</p>
     *
     * <ul>
     *     <li>HUB_ADMIN → HUB 소속</li>
     *     <li>DELIVERY_MANAGER → HUB 소속</li>
     *     <li>COMPANY_MANAGER → COMPANY 소속</li>
     *     <li>MASTER_ADMIN → 일반 회원가입 불가</li>
     * </ul>
     */
    private void validateSignUp(SignUpCommand command) {
        if (!command.role().isSignUpAllowed()) {
            throw new AuthException(
                    AuthErrorCode.MASTER_ADMIN_SIGN_UP_NOT_ALLOWED
            );
        }

        boolean valid = switch (command.role()) {
            case HUB_ADMIN, DELIVERY_MANAGER ->
                    command.affiliationType()
                            == AffiliationType.HUB
                            && command.affiliationId() != null;

            case COMPANY_MANAGER -> command.affiliationType()
                    == AffiliationType.COMPANY
                    && command.affiliationId() != null;

            case MASTER_ADMIN -> false;
        };

        if (!valid) {
            throw new AuthException(
                    AuthErrorCode.INVALID_AFFILIATION
            );
        }
    }

    /**
     * 로그인 대상 사용자의 삭제 여부와 가입 승인 상태를 검증합니다.
     *
     * <p>APPROVED 상태의 활성 사용자만 로그인할 수 있습니다.</p>
     */
    private void validateLoginUser(
            InternalUserAuthInfoResponse user
    ) {
        /*
         * User Service의 auth-info API는 현재 Soft Delete되지 않은 사용자를
         * 조회하도록 구현되어 있지만, 내부 API 계약 변경 등에 대비하여
         * Auth에서도 방어적으로 deleted 값을 확인합니다.
         */
        if (user.deleted()) {
            throw new AuthException(
                    AuthErrorCode.DEACTIVATED_USER
            );
        }

        if (user.userStatus() == UserStatus.REJECTED) {
            throw new AuthException(
                    AuthErrorCode.USER_REJECTED
            );
        }

        if (!user.userStatus().isLoginAllowed()) {
            throw new AuthException(
                    AuthErrorCode.USER_NOT_APPROVED
            );
        }
    }

    /**
     * 요청받은 Refresh Token의 hash와
     * Redis에 저장된 Refresh Token hash가 일치하는지 검증합니다.
     *
     * <p>Refresh Token 원문은 Redis에 저장하지 않습니다.</p>
     */
    private void validateSavedRefreshToken(
            UUID userId,
            String refreshToken
    ) {
        String savedHash = refreshTokenRepository
                .findTokenHashByUserId(userId)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
                ));

        String requestHash =
                tokenHashProvider.hash(refreshToken);

        if (!savedHash.equals(requestHash)) {
            throw new AuthException(
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );
        }
    }

    /**
     * Access Token과 Refresh Token이
     * 동일한 사용자 및 동일한 로그인 세션에서 발급되었는지 검증합니다.
     */
    private void validateSameTokenOwner(
            JwtClaims refreshClaims,
            JwtClaims accessClaims
    ) {
        boolean sameUser = refreshClaims.userId()
                .equals(accessClaims.userId());

        boolean sameSession = refreshClaims.sessionId()
                .equals(accessClaims.sessionId());

        if (!sameUser || !sameSession) {
            throw new AuthException(
                    AuthErrorCode.TOKEN_OWNER_MISMATCH
            );
        }
    }

    /**
     * JWT의 sessionId와 Redis에 저장된
     * 현재 사용자 sessionId가 일치하는지 검증합니다.
     *
     * <p>새로운 로그인으로 sessionId가 교체된 경우
     * 이전 Refresh Token의 sessionId는 이 검증을 통과하지 못합니다.</p>
     */
    private void validateCurrentSession(
            UUID userId,
            UUID sessionId
    ) {
        UUID savedSessionId = sessionRepository
                .findSessionIdByUserId(userId)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.INVALID_REFRESH_TOKEN
                ));

        if (!savedSessionId.equals(sessionId)) {
            throw new AuthException(
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );
        }
    }

    /**
     * Refresh Token을 해시한 후
     * Refresh Token의 만료 시간과 함께 Redis에 저장합니다.
     */
    private void saveRefreshToken(
            UUID userId,
            String refreshToken
    ) {
        refreshTokenRepository.save(
                new RefreshToken(
                        userId,
                        tokenHashProvider.hash(refreshToken),
                        jwtProvider.getRefreshTokenExpiration()
                )
        );
    }

    /**
     * 현재 로그인 세션을 Redis에 저장합니다.
     *
     * <p>Session의 TTL은 Refresh Token의 수명과 동일하게 설정합니다.</p>
     */
    private void saveSession(
            UUID userId,
            UUID sessionId
    ) {
        sessionRepository.save(
                userId,
                sessionId,
                jwtProvider.getRefreshTokenExpiration()
        );
    }

    /**
     * 발급한 TokenPair를 API 응답용 TokenResult로 변환합니다.
     */
    private TokenResult toTokenResult(
            TokenPair tokenPair
    ) {
        return TokenResult.bearer(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                jwtProvider
                        .getAccessTokenExpiration()
                        .toSeconds()
        );
    }

    /**
     * User Service의 공통 응답에서 필수 데이터를 추출합니다.
     *
     * <p>응답 자체가 없거나 실패 응답이거나 데이터가 누락된 경우
     * 정상적인 내부 API 응답으로 간주하지 않습니다.</p>
     */
    private <T> T requireData(
            CommonResponse<T> response
    ) {
        if (response == null
                || !response.success()
                || response.data() == null) {
            throw new AuthException(
                    AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED
            );
        }

        return response.data();
    }
}