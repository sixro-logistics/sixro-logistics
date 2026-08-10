package com.sixro.logistics.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * User Service 실행 클래스입니다.
 *
 * <p>JPA Auditing, Feign Client 및
 * Outbox 이벤트 Polling을 위한 Scheduling을 활성화합니다.</p>
 */

@EnableJpaAuditing(auditorAwareRef = "customAuditorAware")
@EnableFeignClients
@EnableScheduling
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
