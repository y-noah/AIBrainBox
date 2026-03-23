package com.example.demo.llm.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IntentResult {
    private String intent;
    private double confidence;
    private String reason;

    @JsonProperty("should_process")
    private boolean shouldProcess;

    @JsonProperty("question_type")
    private QuestionType questionType;

    private String originalQuestion; // 记录原始问题

    // 无参构造函数（Jackson 需要）
    public IntentResult() {}

    public String getOriginalQuestion() {
        return originalQuestion;
    }

    public void setOriginalQuestion(String originalQuestion) {
        this.originalQuestion = originalQuestion;
    }

    // Getters
    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }

    public boolean isShouldProcess() {
        return shouldProcess;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    // Setters（Jackson 需要）
    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setShouldProcess(boolean shouldProcess) {
        this.shouldProcess = shouldProcess;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
}