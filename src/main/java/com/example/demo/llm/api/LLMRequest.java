package com.example.demo.llm.api;

public class LLMRequest {
    private final String systemPrompt;  // 新增
    private final String userInput;

    /**
     * 只传用户输入的构造方法（兼容旧代码）
     */
    public LLMRequest(String userInput) {
        this(null, userInput);
    }

    /**
     * 传入自定义 SystemPrompt 的构造方法
     */
    public LLMRequest(String systemPrompt, String userInput) {
        this.systemPrompt = systemPrompt;
        this.userInput = userInput;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String userInput() {
        return userInput;
    }

    /**
     * 判断是否有自定义 SystemPrompt
     */
    public boolean hasSystemPrompt() {
        return systemPrompt != null && !systemPrompt.isBlank();
    }
}