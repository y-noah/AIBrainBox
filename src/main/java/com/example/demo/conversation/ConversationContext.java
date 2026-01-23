package com.example.demo.conversation;

import com.example.demo.capability.CapabilityMatchResult;
import com.example.demo.llm.api.IntentResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对话上下文
 * 存储一个会话的所有状态信息
 */
public class ConversationContext {
    private final String conversationId;
    private final LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;

    private final List<ConversationTurn> turns;  // 对话轮次
    private ConversationState state;             // 当前状态

    // 待补充信息的上下文
    private CapabilityMatchResult pendingMatch;  // 等待补充参数的匹配结果
    private IntentResult pendingIntent;          // 等待补充参数时的意图

    public ConversationContext() {
        this.conversationId = UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
        this.turns = new ArrayList<>();
        this.state = ConversationState.IDLE;
    }

    /**
     * 判断是否在等待用户补充信息
     */
    public boolean isAwaitingClarification() {
        return state == ConversationState.AWAITING_CLARIFICATION
                && pendingMatch != null;
    }

    /**
     * 设置为等待澄清状态
     */
    public void awaitClarification(IntentResult intent, CapabilityMatchResult match) {
        this.state = ConversationState.AWAITING_CLARIFICATION;
        this.pendingIntent = intent;
        this.pendingMatch = match;
        this.lastUpdateTime = LocalDateTime.now();
    }

    /**
     * 添加一轮对话
     */
    public void addTurn(ConversationTurn turn) {
        this.turns.add(turn);
        this.lastUpdateTime = LocalDateTime.now();
    }

    /**
     * 清除等待状态（任务完成或放弃）
     */
    public void clearPending() {
        this.state = ConversationState.IDLE;
        this.pendingMatch = null;
        this.pendingIntent = null;
    }

    /**
     * 判断是否超时（超过 5 分钟没有新消息）
     */
    public boolean isExpired() {
        return LocalDateTime.now().minusMinutes(5).isAfter(lastUpdateTime);
    }

    // Getters
    public String getConversationId() {
        return conversationId;
    }

    public List<ConversationTurn> getTurns() {
        return List.copyOf(turns);
    }

    public ConversationState getState() {
        return state;
    }

    public CapabilityMatchResult getPendingMatch() {
        return pendingMatch;
    }

    public IntentResult getPendingIntent() {
        return pendingIntent;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }
}