package com.sixro.logistics.auth.infrastructure.jwt;

import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.auth.domain.model.TokenPair;
import com.sixro.logistics.auth.domain.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * RSA 키 기반 JWT 발급과 검증을 담당합니다.
 *
 * <p>Access Token과 Refresh Token에 사용자 ID, 권한, 토큰 유형 및 JWT ID를 포함하며,
 * 토큰 검증 시 서명, 발급자, 만료 시간과 토큰 유형을 확인합니다.</p>
 *
 * <p>비밀키는 토큰 발급에만 사용하고 공개키는 토큰 검증에 사용합니다.</p>
 * <p>
 * TODO(auth): 운영 환경의 JWT 키 교체를 지원하도록 kid 헤더와
 *  - 복수 공개키 또는 JWKS 기반 키 조회 방식을 검토한다.
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties properties;
    private final RsaPrivateKeyLoader privateKeyLoader;
    private final RsaPublicKeyLoader publicKeyLoader;

    // 동일한 사용자 정보로 Access Token과 Refresh Token을 발급합니다.
    public TokenPair issueTokenPair(
            UUID userId,
            UserRole role
    ) {
        return new TokenPair(
                createToken(
                        userId,
                        role,
                        ACCESS_TOKEN_TYPE,
                        properties.accessTokenExpiration()
                ),
                createToken(
                        userId,
                        role,
                        REFRESH_TOKEN_TYPE,
                        properties.refreshTokenExpiration()
                )
        );
    }

    // Refresh Token의 서명, 발급자, 만료 시간 및 토큰 유형을 검증합니다.
    public JwtClaims parseRefreshToken(String token) {
        Claims claims = parse(
                token,
                AuthErrorCode.EXPIRED_REFRESH_TOKEN,
                AuthErrorCode.INVALID_REFRESH_TOKEN
        );

        validateTokenType(
                claims,
                REFRESH_TOKEN_TYPE,
                AuthErrorCode.INVALID_REFRESH_TOKEN
        );

        return toJwtClaims(
                claims,
                AuthErrorCode.INVALID_REFRESH_TOKEN
        );
    }

    // Access Token의 서명, 발급자, 만료 시간 및 토큰 유형을 검증합니다.
    public JwtClaims parseAccessToken(String token) {
        Claims claims = parse(
                token,
                AuthErrorCode.EXPIRED_ACCESS_TOKEN,
                AuthErrorCode.INVALID_ACCESS_TOKEN
        );

        validateTokenType(
                claims,
                ACCESS_TOKEN_TYPE,
                AuthErrorCode.INVALID_ACCESS_TOKEN
        );

        return toJwtClaims(
                claims,
                AuthErrorCode.INVALID_ACCESS_TOKEN
        );
    }

    public Duration getAccessTokenExpiration() {
        return properties.accessTokenExpiration();
    }

    public Duration getRefreshTokenExpiration() {
        return properties.refreshTokenExpiration();
    }

    // 토큰 유형과 만료 시간을 적용하여 RSA SHA-256 방식의 JWT를 생성합니다.
    private String createToken(
            UUID userId,
            UserRole role,
            String tokenType,
            Duration expiration
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);

        PrivateKey privateKey =
                privateKeyLoader.getPrivateKey();

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 공개키로 JWT 서명과 발급자를 검증하고 Claims를 추출합니다.
     *
     * <p>만료 오류와 그 밖의 유효하지 않은 토큰 오류를 구분합니다.</p>
     */
    private Claims parse(
            String token,
            AuthErrorCode expiredError,
            AuthErrorCode invalidError
    ) {
        try {
            PublicKey publicKey =
                    publicKeyLoader.getPublicKey();

            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException exception) {
            throw new AuthException(
                    expiredError,
                    exception
            );

        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthException(
                    invalidError,
                    exception
            );
        }
    }

    // Access Token과 Refresh Token이 서로의 용도로 사용되는 것을 차단합니다.
    private void validateTokenType(
            Claims claims,
            String expectedTokenType,
            AuthErrorCode errorCode
    ) {
        String tokenType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        if (!expectedTokenType.equals(tokenType)) {
            throw new AuthException(errorCode);
        }
    }

    // JWT 라이브러리의 Claims를 애플리케이션 전용 자료형으로 변환합니다.
    private JwtClaims toJwtClaims(
            Claims claims,
            AuthErrorCode errorCode
    ) {
        try {
            return new JwtClaims(
                    UUID.fromString(claims.getSubject()),
                    UserRole.valueOf(
                            claims.get(
                                    ROLE_CLAIM,
                                    String.class
                            )
                    ),
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );
        } catch (
                IllegalArgumentException
                | NullPointerException exception
        ) {
            throw new AuthException(
                    errorCode,
                    exception
            );
        }
    }
}