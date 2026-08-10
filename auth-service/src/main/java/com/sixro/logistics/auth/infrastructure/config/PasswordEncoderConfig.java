package com.sixro.logistics.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 사용자 비밀번호 암호화와 일치 검증에 사용할 PasswordEncoder를 설정합니다.
 *
 * <p>비밀번호는 복호화할 수 없는 BCrypt 해시로 저장합니다.</p>
 */
@Configuration
public class PasswordEncoderConfig {

    // BCrypt 기반 비밀번호 인코더를 제공합니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}