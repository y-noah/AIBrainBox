package com.example.demo.llm.impl;

import com.example.demo.application.SystemPrompts;
import com.example.demo.llm.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class OllamaClient implements LLMClient {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public OllamaClient(@Value("${ollama.base-url}") String baseUrl,
                        @Value("${ollama.model}") String model,
                        @Value("${ollama.timeout:60s}") Duration timeout) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    @Retryable(
            value = {RetryableLLMException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public List<Float> embed(String text) {
        try {
            OllamaEmbeddingResponse response = webClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(Map.of(
                            "model", model,
                            "prompt", text
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.UNAUTHORIZED) {
                            return clientResponse.createException().map(e -> new NonRetryableLLMException("Unauthorized", e));
                        }
                        if (clientResponse.statusCode().is4xxClientError()) {
                            return clientResponse.createException().map(e -> new NonRetryableLLMException("Bad Request", e));
                        }
                        return clientResponse.createException().map(e -> new RetryableLLMException("Server Error", e));
                    })
                    .bodyToMono(OllamaEmbeddingResponse.class)
                    .timeout(timeout)
                    .block();

            if (response == null || response.getEmbedding() == null || response.getEmbedding().isEmpty()) {
                throw new RetryableLLMException("Empty embedding response");
            }
            return response.getEmbedding();
        } catch (Exception e) {
            throw handleException(e, "Embedding");
        }
    }

    @Override
    @Retryable(
            value = {RetryableLLMException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public LLMResult generate(LLMRequest request) {
        try {
            System.out.println("\n[Ollama] Calling /api/generate...");
            
            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(Map.of(
                            "model", model,
                            "prompt", request.userInput(),
                            "stream", false
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.UNAUTHORIZED) {
                            return clientResponse.createException().map(e -> new NonRetryableLLMException("Unauthorized", e));
                        }
                        return clientResponse.createException().map(e -> new RetryableLLMException("Server Error", e));
                    })
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();

            if (response == null || response.isBlank()) {
                throw new RetryableLLMException("Empty generate response");
            }

            return LLMResult.success(response);
        } catch (Exception e) {
            throw handleException(e, "Generate");
        }
    }

    @Override
    @Retryable(
            value = {RetryableLLMException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public LLMResult chat(LLMRequest request) {
        try {
            String systemPrompt = request.hasSystemPrompt()
                    ? request.systemPrompt()
                    : SystemPrompts.INTENT_JSON_ONLY;

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", request.userInput()));

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "stream", false
            );

            System.out.println("\n[Ollama] Calling /api/chat...");
            
            String response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.UNAUTHORIZED) {
                            return clientResponse.createException().map(e -> new NonRetryableLLMException("Unauthorized", e));
                        }
                        return clientResponse.createException().map(e -> new RetryableLLMException("Server Error", e));
                    })
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();

            if (response == null || response.isBlank()) {
                throw new RetryableLLMException("Empty chat response");
            }

            return LLMResult.success(response);
        } catch (Exception e) {
            throw handleException(e, "Chat");
        }
    }

    /**
     * 统一异常处理逻辑
     */
    private RuntimeException handleException(Exception e, String context) {
        // 如果已经是自定义异常，直接抛出
        if (e instanceof RetryableLLMException || e instanceof NonRetryableLLMException) {
            return (RuntimeException) e;
        }

        // 检查超时
        if (e instanceof TimeoutException || (e.getCause() != null && e.getCause() instanceof TimeoutException)) {
            System.err.println("[Ollama] " + context + " timeout detected. Retrying...");
            return new RetryableLLMException(context + " timeout", e);
        }

        // 检查网络异常
        if (e instanceof WebClientRequestException || e instanceof IOException) {
            System.err.println("[Ollama] " + context + " network error: " + e.getMessage() + ". Retrying...");
            return new RetryableLLMException(context + " network error", e);
        }

        // 其他未知异常视为不可重试，走兜底
        System.err.println("[Ollama] " + context + " unknown error: " + e.getMessage());
        return new NonRetryableLLMException(context + " unknown error", e);
    }

    // ========== Recover 方法，当所有重试都失败后执行 ==========

    @Recover
    public LLMResult recover(RetryableLLMException e, LLMRequest request) {
        System.err.println("[Ollama] Max retries exceeded. Final error: " + e.getMessage());
        if (e.getMessage().contains("timeout")) {
            return LLMResult.failure(LLMError.TIMEOUT);
        }
        return LLMResult.failure(LLMError.MAX_RETRIES_EXCEEDED);
    }

    @Recover
    public List<Float> recoverEmbed(RetryableLLMException e, String text) {
        System.err.println("[Ollama] Embed max retries exceeded. Final error: " + e.getMessage());
        return Collections.emptyList();
    }

    @Recover
    public LLMResult recoverNonRetryable(NonRetryableLLMException e, LLMRequest request) {
        System.err.println("[Ollama] Non-retryable error detected: " + e.getMessage());
        if (e.getMessage().contains("Unauthorized")) {
            return LLMResult.failure(LLMError.AUTHENTICATION_FAILED);
        }
        if (e.getMessage().contains("Bad Request")) {
            return LLMResult.failure(LLMError.BAD_REQUEST);
        }
        return LLMResult.failure(LLMError.UNKNOWN_ERROR);
    }
}