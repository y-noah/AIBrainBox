package com.example.demo.llm.impl;

import com.example.demo.application.SystemPrompts;
import com.example.demo.llm.api.LLMClient;
import com.example.demo.llm.api.LLMError;
import com.example.demo.llm.api.LLMRequest;
import com.example.demo.llm.api.LLMResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaClient implements LLMClient {

    private final ChatClient chatClient;
    private final String model;

    public OllamaClient(ChatClient.Builder chatClientBuilder,
                        @Value("${spring.ai.ollama.chat.options.model}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.model = model;
    }

    @Override
    public LLMResult generate(LLMRequest request) {
        try {
            System.out.println("====== 大模型请求（回答阶段）======");
            System.out.println("模型: " + model);
            System.out.println(request.userInput());
            System.out.println("=====================================");

            String response = chatClient.prompt()
                    .user(request.userInput())
                    .call()
                    .content();

            System.out.println("====== 大模型响应（回答阶段）======");
            System.out.println(response);
            System.out.println("=====================================");

            if (response == null || response.isBlank()) {
                return LLMResult.failure(LLMError.EMPTY_RESPONSE);
            }

            return LLMResult.success(response);
        } catch (RuntimeException e) {
            if (isTimeout(e)) {
                return LLMResult.failure(LLMError.TIMEOUT);
            }
            if (isConnectionError(e)) {
                return LLMResult.failure(LLMError.CONNECTION_FAILED);
            }
            return LLMResult.failure(LLMError.UNKNOWN_ERROR);
        }
    }

    @Override
    public LLMResult chat(LLMRequest request) {
        try {
            System.out.println("====== 大模型请求（意图判定阶段）======");
            System.out.println("模型: " + model);
            System.out.println("系统提示词: " + SystemPrompts.INTENT_JSON_ONLY);
            System.out.println("用户输入: " + request.userInput());
            System.out.println("=======================================");

            String response = chatClient.prompt()
                    .system(SystemPrompts.INTENT_JSON_ONLY)
                    .user(request.userInput())
                    .call()
                    .content();

            System.out.println("====== 大模型响应（意图判定阶段）======");
            System.out.println(response);
            System.out.println("=======================================");

            if (response == null || response.isBlank()) {
                return LLMResult.failure(LLMError.EMPTY_RESPONSE);
            }

            return LLMResult.success(response);
        } catch (RuntimeException e) {
            if (isTimeout(e)) {
                return LLMResult.failure(LLMError.TIMEOUT);
            }
            if (isConnectionError(e)) {
                return LLMResult.failure(LLMError.CONNECTION_FAILED);
            }
            return LLMResult.failure(LLMError.UNKNOWN_ERROR);
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            String type = current.getClass().getName().toLowerCase();
            if (message.contains("timeout") || type.contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isConnectionError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (message.contains("connection")
                    || message.contains("refused")
                    || message.contains("failed to connect")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
