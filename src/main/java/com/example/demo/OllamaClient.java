package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OllamaClient {

    private final WebClient webClient;
    private final String model;

    public OllamaClient(@Value("${ollama.base-url}") String baseUrl,
                        @Value("${ollama.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
    }

    public String chat(String prompt) {
        return webClient.post()
                .uri("/api/chat")
                .bodyValue("""
                {
                  "model": "%s",
                  "messages": [
                    { "role": "user", "content": "%s" }
                  ],
                  "stream": false
                }
                """.formatted(model, prompt))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }


    public String generate(String prompt) {
        return webClient.post()
                .uri("/api/generate")
                .bodyValue("""
                {
                  "model": "%s",
                  "prompt": "%s",
                  "stream": false
                }
                """.formatted(model, prompt))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
