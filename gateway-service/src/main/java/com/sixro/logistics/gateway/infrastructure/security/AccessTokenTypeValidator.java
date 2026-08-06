package com.sixro.logistics.gateway.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Refresh Token이 일반 API 인증에 사용되지 않도록
 * JWT의 tokenType Claim을 검증합니다.
 */
public class AccessTokenTypeValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String TOKEN_TYPE_CLAIM =
            "tokenType";

    private static final String ACCESS_TOKEN_TYPE =
            "ACCESS";

    private static final OAuth2Error INVALID_TOKEN_TYPE =
            new OAuth2Error(
                    "invalid_token",
                    "Access Token이 아닙니다.",
                    null
            );

    @Override
    public OAuth2TokenValidatorResult validate(
            Jwt jwt
    ) {
        String tokenType = jwt.getClaimAsString(
                TOKEN_TYPE_CLAIM
        );

        if (ACCESS_TOKEN_TYPE.equals(tokenType)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(
                INVALID_TOKEN_TYPE
        );
    }
}