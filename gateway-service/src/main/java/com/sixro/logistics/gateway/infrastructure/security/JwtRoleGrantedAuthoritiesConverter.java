package com.sixro.logistics.gateway.infrastructure.security;

import com.sixro.logistics.common.constant.JwtClaimConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

/**
 * JWT의 role Claim을 Spring Security의 GrantedAuthority로 변환합니다.
 *
 * <p>예를 들어 JWT의 role 값이 MASTER_ADMIN이면
 * ROLE_MASTER_ADMIN 권한으로 변환합니다.</p>
 */
public class JwtRoleGrantedAuthoritiesConverter
        implements Converter<Jwt, Flux<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Flux<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaimAsString(JwtClaimConstants.ROLE);

        if (role == null || role.isBlank()) {
            return Flux.empty();
        }

        return Flux.just(
                new SimpleGrantedAuthority(
                        ROLE_PREFIX + role
                )
        );
    }
}