package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.ApplicationRunner;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     * 启动即调用 Ollama
     */
    @Bean
    public ApplicationRunner runner(OllamaClient ollamaClient) {
        return args -> {

            // String response = ollamaClient.chat("用一句话介绍你自己");

            String response = ollamaClient.generate("用一句话介绍你自己");

            System.out.println("===== Ollama 返回 =====");
            System.out.println(response);
        };
    }
}