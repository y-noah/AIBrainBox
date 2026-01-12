package com.example.demo.llm.api;

public enum QuestionType {
    /**
     * 确定性查询：需要查询实时的确定性数据
     * 例如：订单状态、账户余额、库存数量
     */
    DETERMINISTIC_QUERY,

    /**
     * 政策知识：询问企业政策、规则、流程
     * 例如：退款政策、售后流程、会员权益
     */
    POLICY_KNOWLEDGE,

    /**
     * 通用问题：与业务相关的一般性问题
     * 例如：什么是区块链、如何理解某个概念
     */
    GENERAL_QUESTION,

    /**
     * 超出范围：与业务无关的问题
     * 例如：讲个笑话、今天天气怎么样
     */
    OUT_OF_SCOPE
}