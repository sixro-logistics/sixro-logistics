package com.sixro.logistics.auth.infrastructure.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT 발급에 필요한 설정값을 관리합니다.
 *
 * @param issuer                 발급자
 * @param privateKeyPath         RSA Private Key 경로
 * @param accessTokenExpiration  Access Token 유효시간
 * @param refreshTokenExpiration Refresh Token 유효시간
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String privateKeyPath,
        @NotBlank String publicKeyPath,
        @NotNull Duration accessTokenExpiration,
        @NotNull Duration refreshTokenExpiration
) {
}