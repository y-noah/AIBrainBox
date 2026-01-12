package com.example.demo.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具能力注册表
 * 管理所有可用的工具能力
 */
@Component
public class CapabilityRegistry {

    private final Map<String, CapabilityDescriptor> capabilities = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数：启动时注册默认的 Mock 工具
     */
    public CapabilityRegistry() {
        registerDefaultCapabilities();
    }

    /**
     * 注册默认的 Mock 工具
     */
    private void registerDefaultCapabilities() {
        // 1. 订单状态查询工具
        register(new CapabilityDescriptor(
                "order.query_status",
                "查询订单状态",
                "根据订单号查询订单的当前处理状态（待支付/待发货/已发货/已完成等）和物流跟踪信息。必须提供完整且准确的订单号。",
                CapabilityType.TOOL,
                List.of(
                        ParameterDefinition.required(
                                "order_id",
                                "string",
                                "订单编号，格式为 ORD 开头加数字，例如：ORD20240115001",
                                "^ORD\\d+$"
                        )
                ),
                List.of(
                        "订单 ORD20240115001 现在什么状态？",
                        "帮我查一下 ORD123456 的物流",
                        "我的订单 ORD999 发货了吗？"
                )
        ));

        // 2. 账户余额查询工具
        register(new CapabilityDescriptor(
                "account.query_balance",
                "查询账户余额",
                "查询指定账户的当前可用余额。需要提供账户ID。返回精确的金额数值。",
                CapabilityType.TOOL,
                List.of(
                        ParameterDefinition.required(
                                "account_id",
                                "string",
                                "账户ID，格式为 ACC 开头加数字，例如：ACC10001",
                                "^ACC\\d+$"
                        )
                ),
                List.of(
                        "账户 ACC10001 还有多少钱？",
                        "帮我查一下 ACC20230 的余额",
                        "ACC30045 账户余额是多少？"
                )
        ));

        // 3. 知识库查询
        register(new CapabilityDescriptor(
                "knowledge_base.query",
                "查询企业政策与规则",
                "查询企业的政策文档、业务规则、流程说明、常见问题等知识库内容。适用于回答关于退款政策、售后流程、会员权益、业务规定等问题。不用于查询具体的订单、金额、账户等实时数据。",
                CapabilityType.KNOWLEDGE_BASE,
                List.of(),  // 知识库不需要参数
                List.of(
                        "你们的退款政策是什么？",
                        "订单可以修改收货地址吗？",
                        "会员有哪些权益？",
                        "售后服务流程是怎样的？"
                )
        ));
    }

    /**
     * 注册一个工具能力
     */
    public void register(CapabilityDescriptor capability) {
        capabilities.put(capability.capabilityId(), capability);
        System.out.println("✅ 注册工具能力: " + capability.capabilityId() + " - " + capability.name());
    }

    /**
     * 根据 ID 获取工具能力
     */
    public CapabilityDescriptor get(String capabilityId) {
        return capabilities.get(capabilityId);
    }

    /**
     * 获取所有工具能力
     */
    public List<CapabilityDescriptor> getAll() {
        return List.copyOf(capabilities.values());
    }

    /**
     * 转换为给模型看的 JSON 格式
     * 只包含模型做语义匹配所需的信息
     */
    public String toJsonForLLM() {
        try {
            List<Map<String, Object>> simplified = capabilities.values().stream()
                    .map(this::simplifyForLLM)
                    .toList();

            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(simplified);
        } catch (Exception e) {
            System.err.println("❌ 转换工具列表为 JSON 失败: " + e.getMessage());
            return "[]";
        }
    }

    /**
     * 简化工具描述，只保留模型需要的字段
     */
    private Map<String, Object> simplifyForLLM(CapabilityDescriptor cap) {
        Map<String, Object> map = new HashMap<>();

        // 基本信息
        map.put("capability_id", cap.capabilityId());
        map.put("name", cap.name());
        map.put("description", cap.description());

        // 参数信息（简化）
        List<Map<String, Object>> params = cap.parameters().stream()
                .map(param -> {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("name", param.name());
                    paramMap.put("type", param.type());
                    paramMap.put("description", param.description());
                    paramMap.put("required", param.required());
                    return paramMap;
                })
                .toList();
        map.put("parameters", params);

        // 示例问题
        map.put("example_questions", cap.exampleQuestions());

        return map;
    }
}
