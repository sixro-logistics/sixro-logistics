package com.sixro.logistics.auth.infrastructure.jwt;

import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.TokenPair;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.common.constant.JwtClaimConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * RSA 기반 Access/Refresh Token의 발급, Claim 구성, 서명 검증과
 * 토큰 유형·만료 오류 변환을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private static final Duration ACCESS_EXPIRATION =
            Duration.ofMinutes(30);
    private static final Duration REFRESH_EXPIRATION =
            Duration.ofHours(8);

    @Mock
    private RsaPrivateKeyLoader privateKeyLoader;

    @Mock
    private RsaPublicKeyLoader publicKeyLoader;

    private KeyPair keyPair;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        lenient().when(privateKeyLoader.getPrivateKey())
                .thenReturn(keyPair.getPrivate());
        lenient().when(publicKeyLoader.getPublicKey())
                .thenReturn(keyPair.getPublic());

        jwtProvider = provider(
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION
        );
    }

    @Test
    @DisplayName("Access와 Refresh Token에 동일한 사용자와 Session 정보를 발급한다")
    void issueTokenPair_containsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        UUID affiliationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        TokenPair tokenPair = jwtProvider.issueTokenPair(
                userId,
                "hubadmin1",
                UserRole.HUB_ADMIN,
                affiliationId,
                AffiliationType.HUB,
                sessionId
        );

        JwtClaims accessClaims =
                jwtProvider.parseAccessToken(tokenPair.accessToken());
        JwtClaims refreshClaims =
                jwtProvider.parseRefreshToken(tokenPair.refreshToken());

        assertThat(accessClaims.userId()).isEqualTo(userId);
        assertThat(refreshClaims.userId()).isEqualTo(userId);
        assertThat(accessClaims.role()).isEqualTo(UserRole.HUB_ADMIN);
        assertThat(refreshClaims.role()).isEqualTo(UserRole.HUB_ADMIN);
        assertThat(accessClaims.sessionId()).isEqualTo(sessionId);
        assertThat(refreshClaims.sessionId()).isEqualTo(sessionId);
        assertThat(accessClaims.jwtId()).isNotBlank();
        assertThat(refreshClaims.jwtId()).isNotBlank();
        assertThat(accessClaims.jwtId())
                .isNotEqualTo(refreshClaims.jwtId());
    }

    @Test
    @DisplayName("Gateway 전달에 필요한 Access Token Claim을 모두 포함한다")
    void issueTokenPair_accessTokenContainsGatewayClaims() {
        UUID userId = UUID.randomUUID();
        UUID affiliationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        TokenPair tokenPair = jwtProvider.issueTokenPair(
                userId,
                "company1",
                UserRole.COMPANY_MANAGER,
                affiliationId,
                AffiliationType.COMPANY,
                sessionId
        );

        Claims claims = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .requireIssuer("auth-service")
                .build()
                .parseSignedClaims(tokenPair.accessToken())
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get(JwtClaimConstants.USERNAME, String.class))
                .isEqualTo("company1");
        assertThat(claims.get(JwtClaimConstants.ROLE, String.class))
                .isEqualTo("COMPANY_MANAGER");
        assertThat(claims.get(
                JwtClaimConstants.AFFILIATION_ID,
                String.class
        )).isEqualTo(affiliationId.toString());
        assertThat(claims.get(
                JwtClaimConstants.AFFILIATION_TYPE,
                String.class
        )).isEqualTo("COMPANY");
        assertThat(claims.get(
                JwtClaimConstants.SESSION_ID,
                String.class
        )).isEqualTo(sessionId.toString());
        assertThat(claims.get(
                JwtClaimConstants.TOKEN_TYPE,
                String.class
        )).isEqualTo("ACCESS");
    }

    @Test
    @DisplayName("Access Token을 Refresh Token으로 사용할 수 없다")
    void parseRefreshToken_rejectsAccessToken() {
        TokenPair tokenPair = issueMasterTokenPair();

        AuthException exception = assertThrows(
                AuthException.class,
                () -> jwtProvider.parseRefreshToken(
                        tokenPair.accessToken()
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 사용할 수 없다")
    void parseAccessToken_rejectsRefreshToken() {
        TokenPair tokenPair = issueMasterTokenPair();

        AuthException exception = assertThrows(
                AuthException.class,
                () -> jwtProvider.parseAccessToken(
                        tokenPair.refreshToken()
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("위변조된 Access Token은 검증에 실패한다")
    void parseAccessToken_rejectsTamperedToken() {
        TokenPair tokenPair = issueMasterTokenPair();

        AuthException exception = assertThrows(
                AuthException.class,
                () -> jwtProvider.parseAccessToken(
                        tokenPair.accessToken() + "corrupted"
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("만료된 Access Token은 만료 오류로 변환한다")
    void parseAccessToken_mapsExpiredToken() {
        JwtProvider expiredProvider = provider(
                Duration.ofSeconds(-1),
                REFRESH_EXPIRATION
        );

        TokenPair tokenPair = expiredProvider.issueTokenPair(
                UUID.randomUUID(),
                "master01",
                UserRole.MASTER_ADMIN,
                null,
                null,
                UUID.randomUUID()
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> expiredProvider.parseAccessToken(
                        tokenPair.accessToken()
                )
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.EXPIRED_ACCESS_TOKEN);
    }

    private TokenPair issueMasterTokenPair() {
        return jwtProvider.issueTokenPair(
                UUID.randomUUID(),
                "master01",
                UserRole.MASTER_ADMIN,
                null,
                null,
                UUID.randomUUID()
        );
    }

    private JwtProvider provider(
            Duration accessExpiration,
            Duration refreshExpiration
    ) {
        JwtProperties properties = new JwtProperties(
                "auth-service",
                "unused-private-key-path",
                "unused-public-key-path",
                accessExpiration,
                refreshExpiration
        );

        return new JwtProvider(
                properties,
                privateKeyLoader,
                publicKeyLoader
        );
    }
}
