package com.example.demo.capability;

/**
 * 工具匹配状态
 */
public enum MatchStatus {
    /**
     * 匹配成功：找到了合适的工具，且参数齐全
     */
    MATCHED,

    /**
     * 无匹配：没有找到合适的工具处理该问题
     */
    NO_MATCH,

    /**
     * 缺少实体：找到了工具，但用户未提供必需的参数
     * 例如：用户说"查订单"，但没给订单号
     */
    MISSING_ENTITY,

    /**
     * 模糊不清：问题描述不清晰，无法确定应该用哪个工具
     * 例如：用户说"帮我查一下"（查什么？）
     */
    AMBIGUOUS
}