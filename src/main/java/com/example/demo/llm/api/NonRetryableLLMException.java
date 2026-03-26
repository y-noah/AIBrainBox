package com.example.demo.llm.api;

/**
 * 不可重试的 LLM 异常
 * 用于 401 鉴权失败、400 参数错误等确定性问题
 */
public class NonRetryableLLMException extends RuntimeException {
    public NonRetryableLLMException(String message) {
        super(message);
    }

    public NonRetryableLLMException(String message, Throwable cause) {
        super(message, cause);
    }
}