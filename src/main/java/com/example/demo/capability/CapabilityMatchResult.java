package com.example.demo.capability;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 工具能力匹配结果
 * 模型在 Stage 2 的输出结构
 */
public record CapabilityMatchResult(
        @JsonProperty("match_status")
        MatchStatus matchStatus,

        @JsonProperty("matched_capability_id")
        String matchedCapabilityId,

        @JsonProperty("extracted_parameters")
        Map<String, Object> extractedParameters,

        double confidence,

        String reason,

        @JsonProperty("user_guidance")
        String userGuidance
) {
    /**
     * 判断是否可以执行
     * 满足条件：状态为 MATCHED 且置信度 >= 0.7
     */
    public boolean isExecutable() {
        return matchStatus == MatchStatus.MATCHED && confidence >= 0.7;
    }

    /**
     * 判断是否需要用户补充信息
     */
    public boolean needsUserInput() {
        return matchStatus == MatchStatus.MISSING_ENTITY
                || matchStatus == MatchStatus.AMBIGUOUS;
    }

    /**
     * 创建匹配成功的结果
     */
    public static CapabilityMatchResult matched(
            String capabilityId,
            Map<String, Object> parameters,
            double confidence,
            String reason) {
        return new CapabilityMatchResult(
                MatchStatus.MATCHED,
                capabilityId,
                parameters,
                confidence,
                reason,
                null
        );
    }

    /**
     * 创建无匹配的结果
     */
    public static CapabilityMatchResult noMatch(String reason) {
        return new CapabilityMatchResult(
                MatchStatus.NO_MATCH,
                null,
                Map.of(),
                0.0,
                reason,
                "抱歉，我无法处理这个问题"
        );
    }

    /**
     * 创建缺少参数的结果
     */
    public static CapabilityMatchResult missingEntity(
            String capabilityId,
            String reason,
            String guidance) {
        return new CapabilityMatchResult(
                MatchStatus.MISSING_ENTITY,
                capabilityId,
                Map.of(),
                0.8,
                reason,
                guidance
        );
    }

    /**
     * 创建模糊不清的结果
     */
    public static CapabilityMatchResult ambiguous(String reason, String guidance) {
        return new CapabilityMatchResult(
                MatchStatus.AMBIGUOUS,
                null,
                Map.of(),
                0.5,
                reason,
                guidance
        );
    }
}