package com.sixro.logistics.gateway.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

public class JwtRoleGrantedAuthoritiesConverter
        implements Converter<Jwt, Flux<GrantedAuthority>> {

    private static final String ROLE_CLAIM = "role";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Flux<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaimAsString(ROLE_CLAIM);

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