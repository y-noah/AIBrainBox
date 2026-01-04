package com.example.demo.llm.api;

public interface LLMClient {
    LLMResult generate(LLMRequest request);

    LLMResult chat(LLMRequest request);
}
