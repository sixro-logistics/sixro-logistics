package com.sixro.logistics.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 인증·인가 기능을 제공하는 Auth Service의 실행 클래스입니다.
 *
 * <p>OpenFeign 클라이언트를 활성화하여 User Service와 통신하고,
 * {@link org.springframework.boot.context.properties.ConfigurationProperties}
 * 기반 설정 클래스를 자동으로 탐색합니다.</p>
 */
@EnableFeignClients
@ConfigurationPropertiesScan
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
