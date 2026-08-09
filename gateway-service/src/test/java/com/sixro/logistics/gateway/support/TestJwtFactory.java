package com.sixro.logistics.gateway.support;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Gateway JWT 통합 테스트에서 사용할
 * RS256 Access Token을 생성하는 테스트 유틸리티입니다.
 */
public final class TestJwtFactory {

    private static final String ISSUER = "auth-service";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String SESSION_ID_CLAIM = "sessionId";

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String DEFAULT_ROLE = "MASTER_ADMIN";

    private TestJwtFactory() {
    }

    /**
     * 기본 Role(MASTER_ADMIN)의 유효한 Access Token을 생성합니다.
     */
    public static String createValidToken(
            String tokenId,
            UUID userId,
            UUID sessionId
    ) throws Exception {
        return createValidToken(
                tokenId,
                userId,
                sessionId,
                DEFAULT_ROLE
        );
    }

    /**
     * 지정한 Role을 가진 유효한 Access Token을 생성합니다.
     */
    public static String createValidToken(
            String tokenId,
            UUID userId,
            UUID sessionId,
            String role
    ) throws Exception {
        Instant now = Instant.now();

        return createToken(
                tokenId,
                userId,
                sessionId,
                role,
                now,
                now.plusSeconds(30 * 60)
        );
    }

    /**
     * 기본 Role(MASTER_ADMIN)의 만료된 Access Token을 생성합니다.
     */
    public static String createExpiredToken(
            String tokenId,
            UUID userId,
            UUID sessionId
    ) throws Exception {
        Instant now = Instant.now();

        return createToken(
                tokenId,
                userId,
                sessionId,
                DEFAULT_ROLE,
                now.minusSeconds(60 * 60),
                now.minusSeconds(60)
        );
    }

    /**
     * Gateway에서 사용하는 Access Token Claim을 포함하여
     * RS256 JWT를 생성합니다.
     */
    private static String createToken(
            String tokenId,
            UUID userId,
            UUID sessionId,
            String role,
            Instant issuedAt,
            Instant expiresAt
    ) throws Exception {
        RSAPrivateKey privateKey = loadPrivateKey();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .jwtID(tokenId)
                .claim(
                        ROLE_CLAIM,
                        role
                )
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(
                        SESSION_ID_CLAIM,
                        sessionId.toString()
                )
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims
        );

        signedJwt.sign(
                new RSASSASigner(privateKey)
        );

        return signedJwt.serialize();
    }

    private static RSAPrivateKey loadPrivateKey()
            throws Exception {

        ClassPathResource privateKeyResource =
                new ClassPathResource(
                        "keys/test-private-key.pem"
                );

        String pem;

        try (InputStream inputStream =
                     privateKeyResource.getInputStream()) {

            pem = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        String encodedKey = pem
                .replace(
                        "-----BEGIN PRIVATE KEY-----",
                        ""
                )
                .replace(
                        "-----END PRIVATE KEY-----",
                        ""
                )
                .replaceAll("\\s", "");

        byte[] decoded =
                Base64.getDecoder().decode(encodedKey);

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(decoded);

        return (RSAPrivateKey) KeyFactory
                .getInstance("RSA")
                .generatePrivate(keySpec);
    }


}