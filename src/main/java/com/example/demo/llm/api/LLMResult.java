package com.example.demo.llm.api;

public class LLMResult {
    private final boolean success;
    private final String rawText;
    private final LLMError error;

    private LLMResult(boolean success, String rawText, LLMError error) {
        this.success = success;
        this.rawText = rawText;
        this.error = error;
    }

    public static LLMResult success(String rawText) {
        return new LLMResult(true, rawText, null);
    }

    public static LLMResult failure(LLMError error) {
        return new LLMResult(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String rawText() {
        return rawText;
    }

    public LLMError error() {
        return error;
    }
}
