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
import com.sixro.logistics.auth.domain.repository.RefreshTokenRepository;
import com.sixro.logistics.auth.infrastructure.client.UserServiceClient;
import com.sixro.logistics.auth.infrastructure.client.request.InternalCreateUserRequest;
import com.sixro.logistics.auth.infrastructure.client.response.InternalCreateUserResponse;
import com.sixro.logistics.auth.infrastructure.client.response.InternalUserAuthInfoResponse;
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
 * <p>인증 흐름을 조정하는 애플리케이션 서비스이며 다음 책임을 가집니다.</p>
 *
 * <ul>
 *     <li>User Service를 통한 사용자 생성 및 인증 정보 조회</li>
 *     <li>비밀번호 암호화 및 일치 여부 검증</li>
 *     <li>Access Token과 Refresh Token 발급</li>
 *     <li>Refresh Token 해시 저장 및 검증</li>
 *     <li>로그아웃한 Access Token의 블랙리스트 등록</li>
 * </ul>
 *
 * <p>사용자 원본 정보는 User Service가 관리하며,
 * Auth Service는 인증에 필요한 정보만 내부 API로 조회합니다.</p>
 *
 */
@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashProvider tokenHashProvider;

    /**
     * 회원가입 정책을 검증하고 User Service에 사용자 생성을 요청합니다.
     *
     * <p>비밀번호는 Auth Service에서 BCrypt로 암호화한 후 전달합니다.
     * 원문 비밀번호는 User Service에 전달하거나 저장하지 않습니다.</p>
     */
    public SignUpResult signUp(SignUpCommand command) {
        validateSignUp(command);

        String encodedPassword = passwordEncoder.encode(
                command.password()
        );

        InternalCreateUserResponse response =
                createUser(command, encodedPassword);

        return new SignUpResult(
                response.userId(),
                response.username(),
                response.userStatus()
        );
    }

    /**
     * 사용자 상태와 비밀번호를 검증하고 새로운 토큰 쌍을 발급합니다.
     *
     * <p>발급한 Refresh Token은 원문이 아닌 해시값으로 Redis에 저장합니다.</p>
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
            //  - 임계치 초과 시 일정 시간 로그인을 제한하는 정책을 추가한다.
            throw new AuthException(
                    AuthErrorCode.INVALID_USERNAME_OR_PASSWORD
            );
        }

        TokenPair tokenPair = jwtProvider.issueTokenPair(
                user.userId(),
                user.role()
        );

        saveRefreshToken(
                user.userId(),
                tokenPair.refreshToken()
        );

        return toTokenResult(tokenPair);
    }

    /**
     * Refresh Token을 검증하고 새로운 Access Token과 Refresh Token을 발급합니다.
     *
     * <p>JWT 자체의 유효성뿐만 아니라 Redis에 저장된 해시값과도 비교합니다.
     * 새 Refresh Token을 저장하면 기존 Refresh Token은 사용할 수 없습니다.</p>
     */
    public TokenResult reissue(
            ReissueTokenCommand command
    ) {
        JwtClaims claims = jwtProvider.parseRefreshToken(
                command.refreshToken()
        );

        // TODO(auth): 이미 교체된 Refresh Token이 다시 사용되면 토큰 탈취 가능성으로 판단하고,
        //  - 사용자에게 발급된 Refresh Token을 모두 폐기하는 재사용 탐지 정책을 검토한다.
        validateSavedRefreshToken(
                claims.userId(),
                command.refreshToken()
        );

        // TODO(auth): User Service에서 현재 사용자 상태와 권한을 다시 조회한 후 토큰을 재발급한다.
        //  - 비활성화·삭제·승인 취소된 사용자의 토큰 재발급을 차단해야 한다.
        //  - Refresh Token 발급 이후 권한이 변경된 경우 현재 권한으로 새 토큰을 발급해야 한다.
        //  - User Service에 userId 기반 내부 인증 정보 조회 API가 추가된 후 적용한다.
        //  - Redis Lua Script 또는 원자적 compare-and-set 방식으로 검증과 교체를 한 번에 처리한다.
        TokenPair newTokenPair =
                jwtProvider.issueTokenPair(
                        claims.userId(),
                        claims.role()
                );

        saveRefreshToken(
                claims.userId(),
                newTokenPair.refreshToken()
        );

        return toTokenResult(newTokenPair);
    }

    /**
     * Refresh Token을 삭제하고 Access Token을 남은 유효 시간 동안 블랙리스트에 등록합니다.
     *
     * <p>다른 사용자의 토큰을 조합한 로그아웃 요청을 차단하기 위해
     * Access Token과 Refresh Token의 사용자 및 권한 정보를 비교합니다.</p>
     */
    public void logout(LogoutCommand command) {
        JwtClaims refreshClaims =
                jwtProvider.parseRefreshToken(
                        command.refreshToken()
                );

        JwtClaims accessClaims =
                jwtProvider.parseAccessToken(
                        command.accessToken()
                );

        validateSameTokenOwner(
                refreshClaims,
                accessClaims
        );

        validateSavedRefreshToken(
                refreshClaims.userId(),
                command.refreshToken()
        );

        // TODO(auth): Refresh Token 삭제와 Access Token 블랙리스트 등록의
        //  - 부분 실패를 방지하도록 Redis Transaction 또는 Lua Script 적용을 검토한다.
        refreshTokenRepository.deleteByUserId(
                refreshClaims.userId()
        );

        Duration remaining = Duration.between(
                Instant.now(),
                accessClaims.expiration()
        );

        // Access Token 블랙리스트 TTL 방어
        if (!remaining.isNegative() && !remaining.isZero()) {
            refreshTokenRepository.blacklistAccessToken(
                    accessClaims.jwtId(),
                    remaining
            );
        }
    }

    /**
     * User Service에 사용자 생성을 요청하고 Feign 오류를 Auth 오류로 변환합니다.
     *
     * <p>HTTP 400은 잘못된 회원가입 요청, 409는 중복 사용자,
     * 그 외 오류는 User Service 통신 실패로 처리합니다.</p>
     */
    // TODO(auth): User Service 호출과 Feign 예외 변환을 UserAuthClientAdapter로 분리한다.
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
            throw convertSignUpException(exception);
        }
    }

    /**
     * User Service의 HTTP 상태 코드를 Auth Service 오류 코드로 변환합니다.
     */
    private AuthException convertSignUpException(
            FeignException exception
    ) {
        // TODO(auth): User Service의 ErrorResponse 본문을 파싱하여 세부 오류 코드로 변환한다.
        //  - 사용자명 중복과 Slack ID 중복을 각각 구분할 수 있도록 한다.
        //  - 여러 Feign 호출에서 같은 변환 규칙을 사용하게 되면 ErrorDecoder 분리를 검토한다.
        return switch (exception.status()) {
            case 400 -> new AuthException(
                    AuthErrorCode.INVALID_SIGN_UP_REQUEST,
                    exception
            );

            case 409 -> new AuthException(
                    AuthErrorCode.DUPLICATE_USER,
                    exception
            );

            default -> new AuthException(
                    AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED,
                    exception
            );
        };
    }

    /**
     * 로그인을 위해 User Service에서 사용자 인증 정보를 조회합니다.
     *
     * <p>사용자 존재 여부가 외부에 노출되지 않도록 404 응답도
     * 아이디 또는 비밀번호 불일치 오류로 변환합니다.</p>
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
     * 가입 가능한 권한인지 확인하고 권한과 소속 유형의 조합을 검증합니다.
     *
     * <p>MASTER_ADMIN은 일반 회원가입으로 생성할 수 없습니다.</p>
     */
    private void validateSignUp(SignUpCommand command) {
        if (!command.role().isSignUpAllowed()) {
            throw new AuthException(
                    AuthErrorCode.MASTER_ADMIN_SIGN_UP_NOT_ALLOWED
            );
        }

        boolean valid = switch (command.role()) {
            case HUB_ADMIN -> command.affiliationType()
                    == AffiliationType.HUB
                    && command.affiliationId() != null;

            case COMPANY_MANAGER -> command.affiliationType()
                    == AffiliationType.COMPANY
                    && command.affiliationId() != null;

            case DELIVERY_MANAGER -> command.affiliationType() != null
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
     * 사용자의 삭제 여부와 가입 승인 상태를 확인합니다.
     *
     * <p>승인 완료 상태의 활성 사용자만 로그인할 수 있습니다.</p>
     */
    private void validateLoginUser(
            InternalUserAuthInfoResponse user
    ) {
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
     * 요청받은 Refresh Token의 해시값과 Redis 저장값을 비교합니다.
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
     * Access Token과 Refresh Token이 동일한 사용자와 권한으로 발급되었는지 확인합니다.
     */
    private void validateSameTokenOwner(
            JwtClaims refreshClaims,
            JwtClaims accessClaims
    ) {
        boolean sameUser = refreshClaims.userId()
                .equals(accessClaims.userId());

        boolean sameRole = refreshClaims.role()
                == accessClaims.role();

        if (!sameUser || !sameRole) {
            throw new AuthException(
                    AuthErrorCode.TOKEN_OWNER_MISMATCH
            );
        }
    }

    /**
     * Refresh Token을 해시한 후 토큰 만료 시간과 함께 저장합니다.
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
     * Bearer 인증 방식의 토큰 결과를 생성합니다.
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