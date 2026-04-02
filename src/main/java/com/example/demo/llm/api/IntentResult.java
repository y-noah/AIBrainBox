package com.example.demo.llm.api;

public class IntentResult {

    private String intent;
    private double confidence;
    private String reason;
    private String tools;

    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }

    public String getTools() {
        return tools;
    }
}
