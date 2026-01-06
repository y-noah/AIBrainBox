package com.example.demo.llm.impl;

import com.example.demo.application.SystemPrompts;
import com.example.demo.llm.api.LLMClient;
import com.example.demo.llm.api.LLMError;
import com.example.demo.llm.api.LLMRequest;
import com.example.demo.llm.api.LLMResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class OllamaClient implements LLMClient {

    private final WebClient webClient;
    private final String model;

    public OllamaClient(@Value("${ollama.base-url}") String baseUrl,
                        @Value("${ollama.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
    }

    @Override
    public LLMResult generate(LLMRequest request) {
        try {
            System.out.println("====== OLLAMA RAW INPUT ======");
            System.out.println(request.userInput());
            System.out.println("================================");

            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue("""
                    {
                      "model": "%s",
                      "prompt": "%s",
                      "stream": false
                    }
                    """.formatted(model, request.userInput()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofMinutes(10));

            if (response == null || response.isBlank()) {
                return LLMResult.failure(LLMError.EMPTY_RESPONSE);
            }

            return LLMResult.success(response);

        } catch (RuntimeException e) {
            if (e.getCause() != null
                    && e.getCause().getMessage() != null
                    && e.getCause().getMessage().contains("Timeout")) {
                return LLMResult.failure(LLMError.TIMEOUT);
            }
            return LLMResult.failure(LLMError.UNKNOWN_ERROR);
        }
    }

    @Override
    public LLMResult chat(LLMRequest request) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", SystemPrompts.INTENT_JSON_ONLY
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", request.userInput()
                            )
                    ),
                    "stream", false
            );

            System.out.println("====== OLLAMA RAW INPUT ======");
            System.out.println(body.get("messages").toString());
            System.out.println("================================");

            String response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofMinutes(10));

            System.out.println("====== OLLAMA RAW RESPONSE ======");
            System.out.println(response);
            System.out.println("================================");

            if (response == null || response.isBlank()) {
                return LLMResult.failure(LLMError.EMPTY_RESPONSE);
            }

            return LLMResult.success(response);

        } catch (RuntimeException e) {
            if (e.getCause() != null
                    && e.getCause().getMessage() != null
                    && e.getCause().getMessage().contains("Timeout")) {
                return LLMResult.failure(LLMError.TIMEOUT);
            }
            return LLMResult.failure(LLMError.UNKNOWN_ERROR);
        }
    }
}
