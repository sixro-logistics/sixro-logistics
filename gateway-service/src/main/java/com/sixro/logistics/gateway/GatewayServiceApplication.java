package com.sixro.logistics.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
@EnableEurekaClient 없어도 실행 가능
현재(Spring Boot 3.x + Spring Cloud 2025.x)에서는 의존성만 추가하면 자동으로 Eureka Client가 활성화
 */
@SpringBootApplication
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

}
