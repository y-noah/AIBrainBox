package com.example.demo.conversation;

import java.time.LocalDateTime;

/**
 * 一轮对话
 */
public record ConversationTurn(
        LocalDateTime timestamp,
        String userInput,           // 用户输入
        String systemResponse,      // 系统响应
        TurnType type              // 轮次类型
) {
    public enum TurnType {
        QUESTION,              // 用户提问
        CLARIFICATION,         // 系统澄清请求
        ANSWER,                // 系统回答
        ERROR                  // 错误响应
    }

    public static ConversationTurn question(String userInput, String systemResponse) {
        return new ConversationTurn(
                LocalDateTime.now(),
                userInput,
                systemResponse,
                TurnType.QUESTION
        );
    }

    public static ConversationTurn clarification(String userInput, String systemResponse) {
        return new ConversationTurn(
                LocalDateTime.now(),
                userInput,
                systemResponse,
                TurnType.CLARIFICATION
        );
    }
}