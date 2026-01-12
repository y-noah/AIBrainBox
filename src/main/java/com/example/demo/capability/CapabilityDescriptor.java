package com.example.demo.capability;

import java.util.List;

/**
 * 工具能力描述
 * 定义一个工具的完整信息，供 AI 理解和匹配
 */
public record CapabilityDescriptor(
        String capabilityId,              // 唯一标识：order.query_status
        String name,                      // 显示名称：查询订单状态
        String description,               // 详细描述：用途、输入、输出
        CapabilityType type,              // 类型：TOOL | WORKFLOW | KNOWLEDGE_BASE
        List<ParameterDefinition> parameters,  // 参数定义列表
        List<String> exampleQuestions     // 示例问题（帮助模型理解适用场景）
) {
    /**
     * 检查是否有必需参数
     */
    public boolean hasRequiredParameters() {
        return parameters.stream().anyMatch(ParameterDefinition::required);
    }

    /**
     * 获取所有必需参数的名称
     */
    public List<String> getRequiredParameterNames() {
        return parameters.stream()
                .filter(ParameterDefinition::required)
                .map(ParameterDefinition::name)
                .toList();
    }
}