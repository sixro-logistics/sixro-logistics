package com.sixro.logistics.gateway.infrastructure.security;

import com.sixro.logistics.common.constant.JwtClaimConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일반 API 인증에는 Access Token만 사용할 수 있도록
 * tokenType Claim 검증 정책을 확인하는 단위 테스트입니다.
 */
class AccessTokenTypeValidatorTest {

    private final AccessTokenTypeValidator validator =
            new AccessTokenTypeValidator();

    @Test
    @DisplayName("tokenType이 ACCESS이면 검증에 성공한다")
    void validate_accessToken_success() {
        Jwt jwt = jwtWithTokenType("ACCESS");

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REFRESH", "UNKNOWN", "", "access"})
    @DisplayName("ACCESS가 아닌 tokenType은 검증에 실패한다")
    void validate_nonAccessToken_failure(String tokenType) {
        Jwt jwt = jwtWithTokenType(tokenType);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.getErrorCode())
                            .isEqualTo("invalid_token");
                    assertThat(error.getDescription())
                            .isEqualTo("Access Token이 아닙니다.");
                });
    }

    @Test
    @DisplayName("tokenType Claim이 없으면 검증에 실패한다")
    void validate_missingTokenType_failure() {
        Jwt jwt = baseBuilder().build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwtWithTokenType(String tokenType) {
        return baseBuilder()
                .claim(JwtClaimConstants.TOKEN_TYPE, tokenType)
                .build();
    }

    private Jwt.Builder baseBuilder() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800));
    }
}
