package com.example.demo.llm.api;

public class IntentResult {

    private String intent;
    private double confidence;
    private String reason;

    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }
}
