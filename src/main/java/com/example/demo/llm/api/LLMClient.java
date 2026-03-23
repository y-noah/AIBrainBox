package com.example.demo.llm.api;

import java.util.List;

public interface LLMClient {
    LLMResult generate(LLMRequest request);

    LLMResult chat(LLMRequest request);

    List<Float> embed(String text);
}
