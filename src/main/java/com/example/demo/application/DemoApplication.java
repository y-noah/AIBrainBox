package com.example.demo.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication(scanBasePackages = "com.example.demo")
public class DemoApplication {
    public static void main(String[] args) {
        System.out.println("APPLICATION STARTING...");
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("APPLICATION STARTED.");
    }
}