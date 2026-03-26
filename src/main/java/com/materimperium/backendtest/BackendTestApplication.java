package com.materimperium.backendtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendTestApplication.class, args);
    }
}
