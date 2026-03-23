package com.example.demo.llm.impl;

import com.example.demo.application.SystemPrompts;
import com.example.demo.llm.api.LLMClient;
import com.example.demo.llm.api.LLMError;
import com.example.demo.llm.api.LLMRequest;
import com.example.demo.llm.api.LLMResult;
import com.example.demo.llm.api.OllamaEmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class OllamaClient implements LLMClient {

    private final WebClient webClient;
    private final String model;

    public OllamaClient(@Value("${ollama.base-url}") String baseUrl,
                        @Value("${ollama.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 增大内存限制以支持较大的 embedding 响应
                .build();
        this.model = model;
    }

    @Override
    public List<Float> embed(String text) {
        try {
            OllamaEmbeddingResponse response = webClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(Map.of(
                            "model", model,
                            "prompt", text
                    ))
                    .retrieve()
                    .bodyToMono(OllamaEmbeddingResponse.class)
                    .block(Duration.ofMinutes(1));

            if (response != null && response.getEmbedding() != null) {
                return response.getEmbedding();
            }
        } catch (Exception e) {
            System.err.println("Embedding error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public LLMResult generate(LLMRequest request) {
        try {
            System.out.println("====== OLLAMA RAW INPUT ======");
            System.out.println(request.userInput());
            System.out.println("================================");

            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(Map.of(
                            "model", model,
                            "prompt", request.userInput(),
                            "stream", false
                    ))
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
            // ========== 核心改动：支持动态 SystemPrompt ==========
            // 如果 request 中有 systemPrompt，使用它；否则使用默认的 INTENT_JSON_ONLY
            String systemPrompt = request.hasSystemPrompt()
                    ? request.systemPrompt()
                    : SystemPrompts.INTENT_JSON_ONLY;

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", request.userInput()));

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "stream", false
            );

            System.out.println("====== OLLAMA RAW INPUT ======");
            System.out.println("System Prompt: " + systemPrompt.substring(0, Math.min(100, systemPrompt.length())) + "...");
            System.out.println("User Input: " + request.userInput());
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