package com.example.demo.conversation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话上下文管理器
 * 管理所有活跃的对话上下文
 */
@Component
public class ConversationManager {

    // 使用 ConcurrentHashMap 支持多线程
    private final Map<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    /**
     * 获取或创建对话上下文
     * 在控制台场景中，sessionId 可以固定为 "console"
     */
    public ConversationContext getOrCreateContext(String sessionId) {
        return contexts.computeIfAbsent(sessionId, id -> new ConversationContext());
    }

    /**
     * 清理过期的上下文
     */
    public void cleanupExpiredContexts() {
        contexts.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 重置上下文（用于测试或重新开始）
     */
    public void resetContext(String sessionId) {
        contexts.remove(sessionId);
    }

    /**
     * 获取活跃对话数量
     */
    public int getActiveContextCount() {
        return contexts.size();
    }
}