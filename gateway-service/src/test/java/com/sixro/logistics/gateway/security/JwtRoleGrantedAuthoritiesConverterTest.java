package com.sixro.logistics.gateway.security;

import com.sixro.logistics.gateway.infrastructure.security.JwtRoleGrantedAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRoleGrantedAuthoritiesConverterTest {

    private final JwtRoleGrantedAuthoritiesConverter converter =
            new JwtRoleGrantedAuthoritiesConverter();

    @Test
    void convertsRoleClaimToSpringSecurityAuthority() {
        Jwt jwt = createJwt("MASTER_ADMIN");

        List<GrantedAuthority> authorities =
                converter.convert(jwt)
                        .collectList()
                        .block();

        assertThat(authorities)
                .isNotNull()
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MASTER_ADMIN");
    }

    @Test
    void returnsEmptyAuthoritiesWhenRoleClaimIsMissing() {
        Jwt jwt = createJwtWithoutRole();

        List<GrantedAuthority> authorities =
                converter.convert(jwt)
                        .collectList()
                        .block();

        assertThat(authorities)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void returnsEmptyAuthoritiesWhenRoleClaimIsBlank() {
        Jwt jwt = createJwt(" ");

        List<GrantedAuthority> authorities =
                converter.convert(jwt)
                        .collectList()
                        .block();

        assertThat(authorities)
                .isNotNull()
                .isEmpty();
    }

    private Jwt createJwt(String role) {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("550e8400-e29b-41d4-a716-446655440000")
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build();
    }

    private Jwt createJwtWithoutRole() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("550e8400-e29b-41d4-a716-446655440000")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build();
    }
}