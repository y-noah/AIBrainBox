package com.example.demo.llm.api;

/**
 * 可重试的 LLM 异常
 * 用于网络超时、连接抖动、空响应等临时性问题
 */
public class RetryableLLMException extends RuntimeException {
    public RetryableLLMException(String message) {
        super(message);
    }

    public RetryableLLMException(String message, Throwable cause) {
        super(message, cause);
    }
}