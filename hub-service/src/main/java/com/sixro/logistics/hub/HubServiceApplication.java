package com.sixro.logistics.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HubServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubServiceApplication.class, args);
    }

}
