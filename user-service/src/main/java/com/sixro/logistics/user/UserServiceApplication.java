package com.sixro.logistics.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service 실행 클래스입니다.
 *
 * <p>QueryDSL의 JPAQueryFactory는 common-module의
 * QuerydslAutoConfiguration을 통해 자동 등록됩니다.</p>
 */
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableFeignClients
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
