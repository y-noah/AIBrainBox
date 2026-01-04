package com.example.demo.llm.api;

public class LLMRequest {
    private final String userInput;

    public LLMRequest(String userInput) {
        this.userInput = userInput;
    }

    public String userInput() {
        return userInput;
    }

}
