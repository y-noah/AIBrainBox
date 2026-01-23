package com.example.demo.conversation;

/**
 * 对话状态
 */
public enum ConversationState {
    /**
     * 空闲：可以接受新问题
     */
    IDLE,

    /**
     * 等待澄清：等待用户补充缺失的参数
     */
    AWAITING_CLARIFICATION,

    /**
     * 执行中：正在执行工具
     */
    EXECUTING,

    /**
     * 已完成：任务完成
     */
    COMPLETED
}