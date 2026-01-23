package com.example.demo.conversation;

import com.example.demo.capability.CapabilityDescriptor;
import com.example.demo.capability.CapabilityMatchResult;
import com.example.demo.capability.CapabilityRegistry;
import com.example.demo.capability.MatchStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 澄清处理器
 * 负责识别用户是否在补充缺失的参数
 */
@Component
public class ClarificationHandler {

    private final CapabilityRegistry capabilityRegistry;

    public ClarificationHandler(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    /**
     * 尝试从用户输入中提取缺失的参数
     */
    public CapabilityMatchResult handleClarification(
            String userInput,
            ConversationContext context) {

        CapabilityMatchResult pendingMatch = context.getPendingMatch();
        CapabilityDescriptor capability = capabilityRegistry.get(
                pendingMatch.matchedCapabilityId()
        );

        if (capability == null) {
            return null;
        }

        // 找出缺失的参数
        String missingParam = findMissingParameter(capability, pendingMatch);
        if (missingParam == null) {
            return null;
        }

        // 尝试从用户输入中提取参数
        String extractedValue = extractParameter(missingParam, userInput);
        if (extractedValue == null) {
            return null;
        }

        // 合并参数
        Map<String, Object> completeParams = new HashMap<>(pendingMatch.extractedParameters());
        completeParams.put(missingParam, extractedValue);

        // 返回新的匹配结果
        return CapabilityMatchResult.matched(
                pendingMatch.matchedCapabilityId(),
                completeParams,
                0.9,
                "用户补充了缺失的参数: " + missingParam
        );
    }

    /**
     * 找出第一个缺失的参数
     */
    private String findMissingParameter(
            CapabilityDescriptor capability,
            CapabilityMatchResult match) {

        for (String paramName : capability.getRequiredParameterNames()) {
            if (!match.extractedParameters().containsKey(paramName)) {
                return paramName;
            }
        }
        return null;
    }

    /**
     * 从用户输入中提取参数
     */
    private String extractParameter(String paramName, String userInput) {
        return switch (paramName) {
            case "order_id" -> extractOrderId(userInput);
            case "account_id" -> extractAccountId(userInput);
            default -> null;
        };
    }

    private String extractOrderId(String input) {
        // 匹配 ORD123 或 123（自动补充 ORD）
        Pattern pattern = Pattern.compile("(ORD)?\\s*(\\d+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String number = matcher.group(2);
            return "ORD" + number;
        }
        return null;
    }

    private String extractAccountId(String input) {
        Pattern pattern = Pattern.compile("(ACC)?\\s*(\\d+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String number = matcher.group(2);
            return "ACC" + number;
        }
        return null;
    }

    /**
     * 生成澄清问题
     */
    public String generateClarificationQuestion(CapabilityMatchResult match) {
        CapabilityDescriptor capability = capabilityRegistry.get(match.matchedCapabilityId());
        if (capability == null) {
            return "请提供更多信息";
        }

        String missingParam = findMissingParameter(capability, match);
        if (missingParam == null) {
            return "请提供更多信息";
        }

        return switch (missingParam) {
            case "order_id" -> "请提供订单号（例如：ORD123 或 123）";
            case "account_id" -> "请提供账户ID（例如：ACC10001 或 10001）";
            default -> "请提供 " + missingParam;
        };
    }
}