package com.example.demo.capability;

/**
 * 工具能力类型
 */
public enum CapabilityType {
    /**
     * 工具插件：确定性的查询或操作
     * 例如：查询订单状态、查询账户余额
     */
    TOOL,

    /**
     * 工作流：多步骤的业务流程
     * 例如：审批流程、退款流程
     */
    WORKFLOW,

    /**
     * 知识库：企业文档、政策、FAQ
     */
    KNOWLEDGE_BASE
}
