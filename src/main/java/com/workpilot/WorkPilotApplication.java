package com.workpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@MapperScan("com.workpilot.mapper")
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class WorkPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkPilotApplication.class, args);
    }
}
