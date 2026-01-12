package com.example.demo.capability;

/**
 * 参数定义
 * 描述工具需要的参数信息
 */
public record ParameterDefinition(
        String name,           // 参数名：order_id
        String type,           // 类型：string | number | boolean
        String description,    // 描述：订单编号，格式为 ORD + 数字
        boolean required,      // 是否必需
        String pattern         // 正则校验（可选）：^ORD\\d+$
) {
    /**
     * 创建必需参数
     */
    public static ParameterDefinition required(String name, String type, String description) {
        return new ParameterDefinition(name, type, description, true, null);
    }

    /**
     * 创建必需参数（带格式校验）
     */
    public static ParameterDefinition required(String name, String type, String description, String pattern) {
        return new ParameterDefinition(name, type, description, true, pattern);
    }

    /**
     * 创建可选参数
     */
    public static ParameterDefinition optional(String name, String type, String description) {
        return new ParameterDefinition(name, type, description, false, null);
    }
}