package com.sixro.logistics.gateway.infrastructure.config;

import com.sixro.logistics.gateway.infrastructure.security.AccessTokenTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Gateway에서 JWT 서명과 프로젝트 전용 Claim을 검증하기 위한
 * ReactiveJwtDecoder를 구성합니다.
 */
@Configuration
public class JwtDecoderConfig {

    private static final String ISSUER = "auth-service";

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value(
                    "${spring.security.oauth2.resourceserver.jwt.public-key-location}"
            )
            Resource publicKeyResource
    ) {
        RSAPublicKey publicKey =
                readPublicKey(publicKeyResource);

        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder
                        .withPublicKey(publicKey)
                        .build();

        OAuth2TokenValidator<Jwt> defaultValidator =
                JwtValidators.createDefaultWithIssuer(
                        ISSUER
                );

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        defaultValidator,
                        new AccessTokenTypeValidator()
                );

        decoder.setJwtValidator(validator);

        return decoder;
    }

    /**
     * X.509 PEM 형식의 RSA 공개키를 읽어 RSAPublicKey로 변환합니다.
     */
    private RSAPublicKey readPublicKey(
            Resource publicKeyResource
    ) {
        try (InputStream inputStream =
                     publicKeyResource.getInputStream()) {

            String pem = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String encodedKey = pem
                    .replace(
                            "-----BEGIN PUBLIC KEY-----",
                            ""
                    )
                    .replace(
                            "-----END PUBLIC KEY-----",
                            ""
                    )
                    .replaceAll("\\s", "");

            byte[] decodedKey =
                    Base64.getDecoder().decode(encodedKey);

            X509EncodedKeySpec keySpec =
                    new X509EncodedKeySpec(decodedKey);

            return (RSAPublicKey) KeyFactory
                    .getInstance("RSA")
                    .generatePublic(keySpec);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "JWT 공개키를 읽을 수 없습니다.",
                    exception
            );
        }
    }
}