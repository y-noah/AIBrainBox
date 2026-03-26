package com.example.demo.llm.api;

public enum LLMError {
    CONNECTION_FAILED,
    EMPTY_RESPONSE,
    UNKNOWN_ERROR,
    TIMEOUT,
    MAX_RETRIES_EXCEEDED,
    AUTHENTICATION_FAILED,
    BAD_REQUEST
}
